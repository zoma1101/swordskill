using System;
using System.Collections.Generic;
using System.IO;

namespace OpenUtauSf2
{
    /// <summary>One playable sample with pre-computed pitch / volume / pan.</summary>
    public class Sf2Layer
    {
        public Sf2Sample Sample;
        public int Start;
        public int End;
        public int LoopStart;
        public int LoopEnd;
        public bool Loop;
        public int RootKey = 60;
        public double Ratio = 1.0;
        public double Gain = 1.0;
        public double Pan;
    }

    public static class Sf2Synth
    {
        public static Sf2Layer CreateLayer(Sf2Zone presetZone, Sf2Zone instZone, Sf2Sample sample, int note, int velocity)
        {
            if (sample == null || instZone == null) return null;

            int frameCount = (int)sample.RawFrames;
            if (frameCount <= 0) return null;

            int div = sample.IsStereo ? 2 : 1;
            int start = (instZone.Get(0, 0) + instZone.Get(4, 0) * 32768) / div;
            int end = frameCount + (instZone.Get(1, 0) + instZone.Get(12, 0) * 32768) / div;
            start = Math.Max(0, Math.Min(frameCount - 1, start));
            end = Math.Max(start + 1, Math.Min(frameCount, end));

            int loopStart = (int)((sample.LoopStart - sample.Start) / div)
                + (instZone.Get(2, 0) + instZone.Get(45, 0) * 32768) / div;
            int loopEnd = (int)((sample.LoopEnd - sample.Start) / div)
                + (instZone.Get(3, 0) + instZone.Get(50, 0) * 32768) / div;
            loopStart = Math.Max(start, Math.Min(end, loopStart));
            loopEnd = Math.Max(start, Math.Min(end, loopEnd));

            int mode = instZone.Get(54, 0);
            bool loop = (mode == 1 || mode == 3) && loopEnd > loopStart;

            int root = instZone.RootKey;

            int coarse = ZoneGet(presetZone, 51, 0) + instZone.Get(51, 0);
            int fine = ZoneGet(presetZone, 52, 0) + instZone.Get(52, 0);
            int scale = instZone.Get(56, 100);
            if (scale <= 0) scale = 100;

            double semitones = (note - root) * (scale / 100.0) + coarse + fine / 100.0;
            double ratio = Math.Pow(2.0, semitones / 12.0);
            if (ratio <= 0 || double.IsNaN(ratio)) ratio = 1.0;

            int attenuation = Math.Max(0, ZoneGet(presetZone, 48, 0) + instZone.Get(48, 0));
            double gain = Math.Pow(10.0, -attenuation / 200.0);

            int pan = ZoneGet(presetZone, 17, 0) + instZone.Get(17, 0);
            pan = Math.Max(-500, Math.Min(500, pan));

            // Very high notes on very low samples become inaudible / aliased, so cap the shift.
            if (ratio > 16.0) ratio = 16.0;

            Sf2Layer layer = new Sf2Layer();
            layer.Sample = sample;
            layer.Start = start;
            layer.End = end;
            layer.LoopStart = loopStart;
            layer.LoopEnd = loopEnd;
            layer.Loop = loop;
            layer.RootKey = root;
            layer.Ratio = ratio;
            layer.Gain = gain;
            layer.Pan = pan / 500.0;
            return layer;
        }

        static int ZoneGet(Sf2Zone zone, int op, int defaultValue)
        {
            return zone == null ? defaultValue : zone.Get(op, defaultValue);
        }

        public static Sf2Layer CreateLayerFromZone(Sf2Zone zone, int note)
        {
            if (zone == null || zone.Sample == null) return null;
            return CreateLayer(null, zone, zone.Sample, note, 100);
        }

        public static Sf2Layer CreateLayerFromSample(Sf2Sample sample, int note)
        {
            if (sample == null) return null;
            Sf2Zone zone = new Sf2Zone();
            zone.Sample = sample;
            return CreateLayer(null, zone, sample, note, 100);
        }

        /// <summary>Mix layers into 16-bit PCM. stereo=false returns mono frames.</summary>
        public static short[] Render(List<Sf2Layer> layers, int sampleRate, double seconds, bool stereo)
        {
            if (sampleRate <= 0) sampleRate = 44100;
            int frames = (int)Math.Ceiling(seconds * sampleRate);
            if (frames < 1) frames = 1;
            if (frames > sampleRate * 60) frames = sampleRate * 60;

            double[] left = new double[frames];
            double[] right = stereo ? new double[frames] : null;

            foreach (Sf2Layer layer in layers)
            {
                if (layer == null || layer.Sample == null || layer.Gain < 0.000001) continue;
                short[] pcm = layer.Sample.GetPcm();
                if (pcm.Length == 0) continue;

                int start = Math.Max(0, Math.Min(pcm.Length - 1, layer.Start));
                int end = Math.Max(start + 1, Math.Min(pcm.Length, layer.End));
                bool loop = layer.Loop;
                int loopStart = Math.Max(start, Math.Min(end, layer.LoopStart));
                int loopEnd = Math.Max(start, Math.Min(end, layer.LoopEnd));
                if (loopEnd <= loopStart) loop = false;

                double step = layer.Ratio * layer.Sample.SampleRate / (double)sampleRate;
                if (step <= 0) continue;

                double pan = Math.Max(-1.0, Math.Min(1.0, layer.Pan));
                double angle = (pan + 1.0) * Math.PI / 4.0;
                double gainL = Math.Cos(angle);
                double gainR = Math.Sin(angle);

                double pos = start;
                for (int k = 0; k < frames; k++)
                {
                    if (pos >= end)
                    {
                        if (!loop) break;
                        double loopLength = loopEnd - loopStart;
                        pos = loopStart + ((pos - loopEnd) % loopLength);
                        if (pos < loopStart) pos = loopStart;
                    }
                    int i0 = (int)pos;
                    if (i0 >= end) break;
                    double frac = pos - i0;
                    int i1 = i0 + 1;
                    if (i1 >= end) i1 = loop ? loopStart : end - 1;
                    double value = pcm[i0] * (1.0 - frac) + pcm[i1] * frac;
                    value *= layer.Gain;

                    if (stereo)
                    {
                        left[k] += value * gainL;
                        right[k] += value * gainR;
                    }
                    else
                    {
                        left[k] += value;
                    }
                    pos += step;
                }
            }

            double peak = 0;
            for (int i = 0; i < frames; i++)
            {
                double a = Math.Abs(left[i]);
                if (a > peak) peak = a;
                if (stereo)
                {
                    a = Math.Abs(right[i]);
                    if (a > peak) peak = a;
                }
            }
            double norm = 1.0;
            if (peak > 32000.0) norm = 32000.0 / peak;

            int fadeIn = Math.Min(sampleRate / 250, frames / 4);       // 4 ms
            int fadeOut = Math.Min(sampleRate / 80, frames / 4);       // 12 ms
            int channels = stereo ? 2 : 1;
            short[] result = new short[frames * channels];
            for (int i = 0; i < frames; i++)
            {
                double fade = 1.0;
                if (fadeIn > 0 && i < fadeIn) fade = i / (double)fadeIn;
                int tail = frames - 1 - i;
                if (fadeOut > 0 && tail < fadeOut) fade *= tail / (double)fadeOut;
                double l = left[i] * norm * fade;
                if (stereo)
                {
                    double r = right[i] * norm * fade;
                    result[i * 2] = ToShort(l);
                    result[i * 2 + 1] = ToShort(r);
                }
                else
                {
                    result[i] = ToShort(l);
                }
            }
            return result;
        }

        static short ToShort(double value)
        {
            if (value > 32767.0) return 32767;
            if (value < -32768.0) return -32768;
            return (short)Math.Round(value);
        }
    }

    public static class WavUtil
    {
        public static byte[] CreateWav(short[] samples, int sampleRate, int channels)
        {
            int dataSize = samples.Length * 2;
            byte[] bytes = new byte[44 + dataSize];
            WriteAscii(bytes, 0, "RIFF");
            WriteInt32(bytes, 4, 36 + dataSize);
            WriteAscii(bytes, 8, "WAVE");
            WriteAscii(bytes, 12, "fmt ");
            WriteInt32(bytes, 16, 16);
            WriteInt16(bytes, 20, 1);
            WriteInt16(bytes, 22, (short)channels);
            WriteInt32(bytes, 24, sampleRate);
            WriteInt32(bytes, 28, sampleRate * channels * 2);
            WriteInt16(bytes, 32, (short)(channels * 2));
            WriteInt16(bytes, 34, 16);
            WriteAscii(bytes, 36, "data");
            WriteInt32(bytes, 40, dataSize);
            for (int i = 0; i < samples.Length; i++)
            {
                bytes[44 + i * 2] = (byte)(samples[i] & 0xFF);
                bytes[45 + i * 2] = (byte)((samples[i] >> 8) & 0xFF);
            }
            return bytes;
        }

        public static void WriteSampleWav(Sf2Sample sample, string path)
        {
            short[] pcm = sample.GetPcm();
            byte[] wav = CreateWav(pcm, sample.SampleRate, 1);
            File.WriteAllBytes(path, wav);
        }

        public static void WriteWav(string path, short[] samples, int sampleRate, int channels)
        {
            File.WriteAllBytes(path, CreateWav(samples, sampleRate, channels));
        }

        static void WriteAscii(byte[] b, int o, string s)
        {
            for (int i = 0; i < s.Length; i++) b[o + i] = (byte)s[i];
        }

        static void WriteInt16(byte[] b, int o, short v)
        {
            b[o] = (byte)(v & 0xFF);
            b[o + 1] = (byte)((v >> 8) & 0xFF);
        }

        static void WriteInt32(byte[] b, int o, int v)
        {
            b[o] = (byte)(v & 0xFF);
            b[o + 1] = (byte)((v >> 8) & 0xFF);
            b[o + 2] = (byte)((v >> 16) & 0xFF);
            b[o + 3] = (byte)((v >> 24) & 0xFF);
        }
    }
}
