using System;
using System.Collections.Generic;
using System.IO;
using System.Text;

namespace OpenUtauSf2
{
    /// <summary>A sample header (shdr) plus lazy access to its 16-bit PCM data.</summary>
    public class Sf2Sample
    {
        public int Index;
        public string Name = "";
        public long Start;
        public long End;
        public long LoopStart;
        public long LoopEnd;
        public int SampleRate = 44100;
        public int OriginalPitch = 60;
        public int PitchCorrection;
        public int SampleLink;
        public int SampleType;

        internal byte[] FileData;
        internal long SmplDataOffset;
        internal long SmplFrames;
        private short[] pcm;

        public bool IsStereo { get { return (SampleType & 0x6) != 0; } }
        public bool IsRom { get { return (SampleType & 0x8000) != 0; } }

        public long RawFrames
        {
            get
            {
                long n = End - Start;
                if (n <= 0) return 0;
                return IsStereo ? n / 2 : n;
            }
        }

        public double LengthSeconds
        {
            get { return RawFrames / (double)Math.Max(1, SampleRate); }
        }

        public short[] GetPcm()
        {
            if (pcm != null) return pcm;
            long frames = SmplFrames;
            long s = Math.Max(0, Math.Min(frames, Start));
            long e = Math.Max(s, Math.Min(frames, End));
            int step = IsStereo ? 2 : 1;
            int n = (int)((e - s) / step);
            short[] result = new short[n];
            long baseOffset = SmplDataOffset;
            for (int i = 0; i < n; i++)
            {
                long b = baseOffset + (s + i * step) * 2;
                if (b + 1 >= FileData.Length) break;
                result[i] = (short)(FileData[b] | (FileData[b + 1] << 8));
            }
            pcm = result;
            return pcm;
        }

        public override string ToString()
        {
            return Name;
        }
    }

    /// <summary>A hydra zone: a merged (global + local) generator map.</summary>
    public class Sf2Zone
    {
        public Dictionary<int, int> Gens = new Dictionary<int, int>();
        public int KeyLow = 0;
        public int KeyHigh = 127;
        public int VelLow = 0;
        public int VelHigh = 127;
        public Sf2Sample Sample;
        public bool IsGlobal;

        public int Get(int op, int defaultValue)
        {
            int value;
            return Gens.TryGetValue(op, out value) ? value : defaultValue;
        }

        public bool Has(int op)
        {
            return Gens.ContainsKey(op);
        }

        public int RootKey
        {
            get
            {
                if (Sample == null) return 60;
                int r = Get(58, -1);
                if (r < 0 || r > 127) r = Sample.OriginalPitch;
                return Math.Max(0, Math.Min(127, r));
            }
        }

        public bool Matches(int key, int velocity)
        {
            return key >= KeyLow && key <= KeyHigh && velocity >= VelLow && velocity <= VelHigh;
        }

        public string KeyRangeText
        {
            get { return Sf2Note.Name(KeyLow) + " - " + Sf2Note.Name(KeyHigh); }
        }
    }

    public class Sf2PresetZone : Sf2Zone
    {
        public Sf2Instrument Instrument;
    }

    public class Sf2Instrument
    {
        public int Index;
        public string Name = "";
        public Dictionary<int, int> GlobalGens = new Dictionary<int, int>();
        public List<Sf2Zone> Zones = new List<Sf2Zone>();

        public override string ToString()
        {
            return Name;
        }
    }

    public class Sf2Preset
    {
        public int Index;
        public string Name = "";
        public int Bank;
        public int Program;
        public Dictionary<int, int> GlobalGens = new Dictionary<int, int>();
        public List<Sf2PresetZone> Zones = new List<Sf2PresetZone>();

        public string DisplayName
        {
            get { return string.Format("{0:000}:{1:000}  {2}", Bank, Program, Name); }
        }

        public override string ToString()
        {
            return DisplayName;
        }
    }

    /// <summary>A parsed SoundFont 2 file.</summary>
    public class Sf2File
    {
        public string FilePath = "";
        public string FileName = "";
        public Dictionary<string, string> Info = new Dictionary<string, string>();
        public List<Sf2Preset> Presets = new List<Sf2Preset>();
        public List<Sf2Instrument> Instruments = new List<Sf2Instrument>();
        public List<Sf2Sample> Samples = new List<Sf2Sample>();

        public string BankName
        {
            get
            {
                string s;
                return Info.TryGetValue("INAM", out s) ? s : FileName;
            }
        }

        public static Sf2File Load(string path)
        {
            byte[] data = File.ReadAllBytes(path);
            Sf2File sf2 = new Sf2File();
            sf2.FilePath = Path.GetFullPath(path);
            sf2.FileName = Path.GetFileNameWithoutExtension(path);
            sf2.Parse(data);
            return sf2;
        }

        void Parse(byte[] data)
        {
            if (data.Length < 12 || Sf2Bin.FourCC(data, 0) != "RIFF")
                throw new InvalidDataException("RIFF ヘッダが見つかりません。SF2 ファイルではありません。");
            if (Sf2Bin.FourCC(data, 8) != "sfbk")
                throw new InvalidDataException("SF2 (sfbk) 形式ではありません。");

            long riffSize = Sf2Bin.U32(data, 4);
            int end = (int)Math.Min(data.Length, 8 + riffSize);
            int smplOffset = -1;
            int smplSize = 0;
            int pdtaOffset = -1;
            int pdtaSize = 0;

            int p = 12;
            while (p + 8 <= end)
            {
                string id = Sf2Bin.FourCC(data, p);
                int size = (int)Sf2Bin.U32(data, p + 4);
                int d = p + 8;
                if (size < 0 || d + size > data.Length) break;
                if (id == "LIST" && size >= 4)
                {
                    string type = Sf2Bin.FourCC(data, d);
                    if (type == "INFO")
                    {
                        ParseInfo(data, d + 4, size - 4);
                    }
                    else if (type == "sdta")
                    {
                        int q = d + 4;
                        int listEnd = d + size;
                        while (q + 8 <= listEnd)
                        {
                            string sid = Sf2Bin.FourCC(data, q);
                            int ssize = (int)Sf2Bin.U32(data, q + 4);
                            if (sid == "smpl")
                            {
                                smplOffset = q + 8;
                                smplSize = ssize;
                            }
                            q += 8 + ssize + (ssize & 1);
                        }
                    }
                    else if (type == "pdta")
                    {
                        pdtaOffset = d + 4;
                        pdtaSize = size - 4;
                    }
                }
                p = d + size + (size & 1);
            }

            if (pdtaOffset < 0)
                throw new InvalidDataException("pdta チャンクが見つかりません。");
            if (smplOffset < 0)
                throw new InvalidDataException("smpl チャンクが見つかりません（サンプルデータがありません）。");

            long smplFrames = smplSize / 2;

            Dictionary<string, int[]> chunks = new Dictionary<string, int[]>();
            int q2 = pdtaOffset;
            int pdtaEnd = pdtaOffset + pdtaSize;
            while (q2 + 8 <= pdtaEnd)
            {
                string id = Sf2Bin.FourCC(data, q2);
                int size = (int)Sf2Bin.U32(data, q2 + 4);
                int d = q2 + 8;
                if (size < 0 || d + size > data.Length) break;
                chunks[id] = new int[] { d, size };
                q2 = d + size + (size & 1);
            }

            ParseSamples(data, chunks, smplOffset, smplFrames);
            ParseInstruments(data, chunks);
            ParsePresets(data, chunks);
        }

        void ParseInfo(byte[] data, int start, int size)
        {
            int p = start;
            int end = start + size;
            while (p + 8 <= end)
            {
                string id = Sf2Bin.FourCC(data, p);
                int len = (int)Sf2Bin.U32(data, p + 4);
                int d = p + 8;
                if (len < 0 || d + len > data.Length) break;
                if (id != "ifil" && id != "iver")
                {
                    string value = Sf2Bin.DecodeString(data, d, len).Trim();
                    if (value.Length > 0) Info[id] = value;
                }
                p = d + len + (len & 1);
            }
        }

        static int RecordCount(Dictionary<string, int[]> chunks, string id, int recordSize)
        {
            int[] c;
            if (!chunks.TryGetValue(id, out c) || recordSize <= 0) return 0;
            return c[1] / recordSize;
        }

        static int RecordOffset(Dictionary<string, int[]> chunks, string id)
        {
            int[] c;
            return chunks.TryGetValue(id, out c) ? c[0] : 0;
        }

        void ParseSamples(byte[] data, Dictionary<string, int[]> chunks, int smplOffset, long smplFrames)
        {
            int count = RecordCount(chunks, "shdr", 46);
            int off = RecordOffset(chunks, "shdr");
            if (count > 0) count--; // terminator
            for (int i = 0; i < count; i++)
            {
                int o = off + i * 46;
                Sf2Sample s = new Sf2Sample();
                s.Index = i;
                s.Name = Sf2Bin.ReadName(data, o, 20);
                s.Start = Sf2Bin.U32(data, o + 20);
                s.End = Sf2Bin.U32(data, o + 24);
                s.LoopStart = Sf2Bin.U32(data, o + 28);
                s.LoopEnd = Sf2Bin.U32(data, o + 32);
                s.SampleRate = (int)Sf2Bin.U32(data, o + 36);
                s.OriginalPitch = data[o + 40];
                s.PitchCorrection = (sbyte)data[o + 41];
                s.SampleLink = Sf2Bin.U16(data, o + 42);
                s.SampleType = Sf2Bin.U16(data, o + 44);
                s.FileData = data;
                s.SmplDataOffset = smplOffset;
                s.SmplFrames = smplFrames;
                if (s.SampleRate <= 0) s.SampleRate = 44100;
                if (s.Name.Length == 0) s.Name = "sample " + i;
                Samples.Add(s);
            }
        }

        void ParseInstruments(byte[] data, Dictionary<string, int[]> chunks)
        {
            int instCount = RecordCount(chunks, "inst", 22);
            int instOff = RecordOffset(chunks, "inst");
            int bagCount = RecordCount(chunks, "ibag", 4);
            int bagOff = RecordOffset(chunks, "ibag");
            int genCount = RecordCount(chunks, "igen", 4);
            int genOff = RecordOffset(chunks, "igen");
            if (instCount > 0) instCount--;
            if (bagCount > 0) bagCount--;
            if (genCount > 0) genCount--;

            for (int i = 0; i < instCount; i++)
            {
                int o = instOff + i * 22;
                Sf2Instrument inst = new Sf2Instrument();
                inst.Index = i;
                inst.Name = Sf2Bin.ReadName(data, o, 20);
                if (inst.Name.Length == 0) inst.Name = "instrument " + i;
                int bagStart = Sf2Bin.U16(data, o + 20);
                int bagEnd = (i + 1 < instCount) ? Sf2Bin.U16(data, o + 42) : bagCount;
                bagEnd = Math.Min(bagEnd, bagCount);

                Dictionary<int, int> global = new Dictionary<int, int>();
                for (int j = bagStart; j < bagEnd; j++)
                {
                    Dictionary<int, int> gens = ReadBagGens(data, bagOff, bagCount, genOff, genCount, j);
                    if (j == bagStart)
                    {
                        global = gens;
                        inst.GlobalGens = gens;
                        continue;
                    }
                    Dictionary<int, int> merged = MergeGens(global, gens);
                    Sf2Zone zone = new Sf2Zone();
                    zone.Gens = merged;
                    ApplyRanges(zone);
                    int sid = zone.Get(53, -1);
                    if (sid >= 0 && sid < Samples.Count)
                    {
                        zone.Sample = Samples[sid];
                        inst.Zones.Add(zone);
                    }
                }
                Instruments.Add(inst);
            }
        }

        void ParsePresets(byte[] data, Dictionary<string, int[]> chunks)
        {
            int phdrCount = RecordCount(chunks, "phdr", 38);
            int phdrOff = RecordOffset(chunks, "phdr");
            int bagCount = RecordCount(chunks, "pbag", 4);
            int bagOff = RecordOffset(chunks, "pbag");
            int genCount = RecordCount(chunks, "pgen", 4);
            int genOff = RecordOffset(chunks, "pgen");
            if (phdrCount > 0) phdrCount--;
            if (bagCount > 0) bagCount--;
            if (genCount > 0) genCount--;

            for (int i = 0; i < phdrCount; i++)
            {
                int o = phdrOff + i * 38;
                Sf2Preset preset = new Sf2Preset();
                preset.Index = i;
                preset.Name = Sf2Bin.ReadName(data, o, 20);
                if (preset.Name.Length == 0) preset.Name = "preset " + i;
                preset.Program = Sf2Bin.U16(data, o + 20);
                preset.Bank = Sf2Bin.U16(data, o + 22);
                int bagStart = Sf2Bin.U16(data, o + 24);
                int bagEnd = (i + 1 < phdrCount) ? Sf2Bin.U16(data, o + 62) : bagCount;
                bagEnd = Math.Min(bagEnd, bagCount);

                Dictionary<int, int> global = new Dictionary<int, int>();
                for (int j = bagStart; j < bagEnd; j++)
                {
                    Dictionary<int, int> gens = ReadBagGens(data, bagOff, bagCount, genOff, genCount, j);
                    if (j == bagStart)
                    {
                        global = gens;
                        preset.GlobalGens = gens;
                        continue;
                    }
                    Dictionary<int, int> merged = MergeGens(global, gens);
                    Sf2PresetZone zone = new Sf2PresetZone();
                    zone.Gens = merged;
                    ApplyRanges(zone);
                    int instId = zone.Get(41, -1);
                    if (instId >= 0 && instId < Instruments.Count)
                    {
                        zone.Instrument = Instruments[instId];
                        preset.Zones.Add(zone);
                    }
                }
                Presets.Add(preset);
            }
        }

        static Dictionary<int, int> ReadBagGens(byte[] data, int bagOff, int bagCount, int genOff, int genCount, int bagIndex)
        {
            Dictionary<int, int> gens = new Dictionary<int, int>();
            int start = Sf2Bin.U16(data, bagOff + bagIndex * 4);
            int next = (bagIndex + 1 <= bagCount) ? Sf2Bin.U16(data, bagOff + (bagIndex + 1) * 4) : genCount;
            if (next < start) next = start;
            next = Math.Min(next, genCount);
            for (int k = start; k < next; k++)
            {
                int g = genOff + k * 4;
                if (g + 4 > data.Length) break;
                int op = Sf2Bin.U16(data, g);
                int value = Sf2Bin.U16(data, g + 2);
                gens[op] = value;
            }
            return gens;
        }

        static Dictionary<int, int> MergeGens(Dictionary<int, int> global, Dictionary<int, int> local)
        {
            Dictionary<int, int> merged = new Dictionary<int, int>();
            foreach (KeyValuePair<int, int> kv in global) merged[kv.Key] = kv.Value;
            foreach (KeyValuePair<int, int> kv in local) merged[kv.Key] = kv.Value;
            return merged;
        }

        static void ApplyRanges(Sf2Zone zone)
        {
            int range;
            if (zone.Gens.TryGetValue(43, out range))
            {
                zone.KeyLow = range & 0xFF;
                zone.KeyHigh = (range >> 8) & 0xFF;
            }
            if (zone.Gens.TryGetValue(44, out range))
            {
                zone.VelLow = range & 0xFF;
                zone.VelHigh = (range >> 8) & 0xFF;
            }
        }

        /// <summary>All layers of a preset that respond to the given key and velocity.</summary>
        public List<Sf2Layer> Resolve(Sf2Preset preset, int note, int velocity)
        {
            List<Sf2Layer> layers = new List<Sf2Layer>();
            foreach (Sf2PresetZone pz in preset.Zones)
            {
                if (!pz.Matches(note, velocity)) continue;
                if (pz.Instrument == null) continue;
                foreach (Sf2Zone iz in pz.Instrument.Zones)
                {
                    if (!iz.Matches(note, velocity)) continue;
                    Sf2Layer layer = Sf2Synth.CreateLayer(pz, iz, iz.Sample, note, velocity);
                    if (layer != null) layers.Add(layer);
                }
            }
            return layers;
        }

        /// <summary>The most useful sample of a preset for turning into a voicebank.</summary>
        public Sf2Zone PickZone(Sf2Preset preset, int preferredNote)
        {
            Sf2Zone best = null;
            long bestFrames = -1;
            foreach (Sf2PresetZone pz in preset.Zones)
            {
                if (pz.Instrument == null) continue;
                foreach (Sf2Zone iz in pz.Instrument.Zones)
                {
                    if (iz.Sample == null) continue;
                    if (!iz.Matches(preferredNote, 100)) continue;
                    long frames = iz.Sample.RawFrames;
                    if (frames > bestFrames)
                    {
                        bestFrames = frames;
                        best = iz;
                    }
                }
            }
            if (best != null) return best;
            foreach (Sf2PresetZone pz in preset.Zones)
            {
                if (pz.Instrument == null) continue;
                foreach (Sf2Zone iz in pz.Instrument.Zones)
                {
                    if (iz.Sample == null) continue;
                    long frames = iz.Sample.RawFrames;
                    if (frames > bestFrames)
                    {
                        bestFrames = frames;
                        best = iz;
                    }
                }
            }
            return best;
        }
    }

    public static class Sf2Note
    {
        static readonly string[] Names = { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };

        public static string Name(int midi)
        {
            int n = ((midi % 12) + 12) % 12;
            int octave = (int)Math.Floor(midi / 12.0) - 1;
            return Names[n] + octave.ToString();
        }

        public static int Clamp(int midi)
        {
            return Math.Max(0, Math.Min(127, midi));
        }
    }

    public static class Sf2Bin
    {
        public static ushort U16(byte[] b, int o)
        {
            return (ushort)(b[o] | (b[o + 1] << 8));
        }

        public static short I16(byte[] b, int o)
        {
            return (short)(b[o] | (b[o + 1] << 8));
        }

        public static uint U32(byte[] b, int o)
        {
            return (uint)(b[o] | (b[o + 1] << 8) | (b[o + 2] << 16) | (b[o + 3] << 24));
        }

        public static int I32(byte[] b, int o)
        {
            return b[o] | (b[o + 1] << 8) | (b[o + 2] << 16) | (b[o + 3] << 24);
        }

        public static string FourCC(byte[] b, int o)
        {
            if (o + 4 > b.Length) return "";
            char[] c = new char[4];
            for (int i = 0; i < 4; i++) c[i] = (char)b[o + i];
            return new string(c);
        }

        public static string ReadName(byte[] b, int o, int len)
        {
            int n = 0;
            while (n < len && b[o + n] != 0) n++;
            return DecodeString(b, o, n).Trim();
        }

        public static string DecodeString(byte[] b, int o, int len)
        {
            if (len <= 0) return "";
            try
            {
                UTF8Encoding strict = new UTF8Encoding(false, true);
                return strict.GetString(b, o, len);
            }
            catch (DecoderFallbackException)
            {
                try { return Encoding.GetEncoding(932).GetString(b, o, len); }
                catch { return Encoding.ASCII.GetString(b, o, len); }
            }
        }
    }
}
