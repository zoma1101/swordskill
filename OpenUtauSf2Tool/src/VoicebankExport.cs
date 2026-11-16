using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Text;

namespace OpenUtauSf2
{
    /// <summary>Turns an SF2 sample into a small OpenUtau classic voicebank.</summary>
    public static class VoicebankExport
    {
        public static string Export(Sf2File sf2, Sf2Preset preset, Sf2Sample sample, string baseDir, List<string> projectLyrics)
        {
            return Export(sf2, preset, sample, baseDir, projectLyrics, null);
        }

        public static string Export(Sf2File sf2, Sf2Preset preset, Sf2Sample sample, string baseDir, List<string> projectLyrics, Sf2Zone zone)
        {
            if (sample == null) throw new InvalidOperationException("書き出すサンプルがありません。");
            if (string.IsNullOrEmpty(baseDir)) throw new ArgumentException("出力先が空です。");
            Directory.CreateDirectory(baseDir);

            string presetName = preset != null ? preset.Name : sample.Name;
            string folder = SanitizeFileName(presetName + " (SF2)");
            string target = Path.Combine(baseDir, folder);
            int suffix = 2;
            while (Directory.Exists(target))
            {
                target = Path.Combine(baseDir, folder + " " + suffix.ToString(CultureInfo.InvariantCulture));
                suffix++;
            }
            Directory.CreateDirectory(target);

            string wavPath = Path.Combine(target, "sample.wav");
            WriteVoiceSampleWav(sample, zone, wavPath);

            List<string> aliases = BuildAliases(projectLyrics, sample.Name);
            StringBuilder oto = new StringBuilder();
            foreach (string alias in aliases)
            {
                oto.Append("sample.wav=");
                oto.Append(alias);
                oto.Append(",0,0,-1,0,0\r\n");
            }
            WriteText(Path.Combine(target, "oto.ini"), oto.ToString());

            StringBuilder character = new StringBuilder();
            character.Append("name=").Append(presetName).Append(" (SF2)\r\n");
            character.Append("author=OpenUtau SF2 Tool\r\n");
            character.Append("web=\r\n");
            character.Append("version=1.0\r\n");
            if (preset != null)
            {
                character.Append("bank=").Append(preset.Bank.ToString("000", CultureInfo.InvariantCulture)).Append("\r\n");
                character.Append("program=").Append(preset.Program.ToString("000", CultureInfo.InvariantCulture)).Append("\r\n");
            }
            character.Append("source=").Append(Path.GetFileName(sf2.FilePath)).Append("\r\n");
            WriteText(Path.Combine(target, "character.txt"), character.ToString());

            StringBuilder readme = new StringBuilder();
            readme.Append("OpenUtau SF2 Tool で生成したボイスバンクです。\r\n\r\n");
            readme.Append("元の SoundFont: ").Append(sf2.FilePath).Append("\r\n");
            if (preset != null)
            {
                readme.Append("プリセット: ").Append(preset.DisplayName).Append("\r\n");
            }
            readme.Append("サンプル: ").Append(sample.Name).Append("\r\n");
            readme.Append("サンプルレート: ").Append(sample.SampleRate).Append(" Hz\r\n");
            readme.Append("長さ: ").Append(sample.LengthSeconds.ToString("0.00", CultureInfo.InvariantCulture)).Append(" 秒\r\n");
            readme.Append("エイリアス数: ").Append(aliases.Count).Append("\r\n\r\n");
            readme.Append("使い方\r\n");
            readme.Append("1. このフォルダを OpenUtau の Singers フォルダに置きます。\r\n");
            readme.Append("2. OpenUtau を再起動するか、歌手一覧を更新します。\r\n");
            readme.Append("3. トラックの歌手として選択し、歌詞を入力します。\r\n\r\n");
            readme.Append("注意\r\n");
            readme.Append("- 1つのサンプルを音程に合わせて伸縮するため、旋律楽器向けです。\r\n");
            readme.Append("- 和音や複数音が混ざったサンプルは、ピッチ検出に失敗することがあります。\r\n");
            readme.Append("- 歌詞ごとのエイリアスは、書き出し時のプロジェクトの歌詞と主要な日本語CV音節です。\r\n");
            readme.Append("- 歌詞を変えた場合はもう一度書き出すと確実です。\r\n");
            WriteText(Path.Combine(target, "README.txt"), readme.ToString());

            return target;
        }

        /// <summary>
        /// Writes the sample as a voicebank WAV. Looped wavetables are unrolled to a few
        /// seconds so they are actually usable as sustained notes.
        /// </summary>
        static void WriteVoiceSampleWav(Sf2Sample sample, Sf2Zone zone, string path)
        {
            Sf2Layer layer = null;
            if (zone != null && zone.Sample == sample)
            {
                layer = Sf2Synth.CreateLayerFromZone(zone, zone.RootKey);
            }
            if (layer == null)
            {
                layer = Sf2Synth.CreateLayerFromSample(sample, sample.OriginalPitch);
            }
            if (layer == null)
            {
                WavUtil.WriteSampleWav(sample, path);
                return;
            }

            // Keep the natural pitch; OpenUtau performs its own pitch detection.
            layer.Ratio = 1.0;
            // Export the raw sample timbre; preset attenuation is not baked in.
            layer.Gain = 1.0;
            int rate = sample.SampleRate > 0 ? sample.SampleRate : 44100;
            double seconds;
            if (layer.Loop)
            {
                double loopSeconds = (layer.LoopEnd - layer.LoopStart) / (double)rate;
                seconds = Math.Max(2.0, loopSeconds * 4.0);
                if (seconds > 8.0) seconds = 8.0;
            }
            else
            {
                seconds = (layer.End - layer.Start) / (double)rate;
                if (seconds < 0.05) seconds = 0.05;
                if (seconds > 30.0) seconds = 30.0;
            }

            List<Sf2Layer> one = new List<Sf2Layer>();
            one.Add(layer);
            short[] pcm = Sf2Synth.Render(one, rate, seconds, false);
            WavUtil.WriteWav(path, pcm, rate, 1);
        }

        static void WriteText(string path, string text)
        {
            File.WriteAllText(path, text, new UTF8Encoding(true));
        }

        public static string SanitizeFileName(string name)
        {
            if (string.IsNullOrEmpty(name)) return "SF2";
            char[] invalid = Path.GetInvalidFileNameChars();
            StringBuilder sb = new StringBuilder();
            foreach (char c in name)
            {
                bool bad = false;
                foreach (char ic in invalid)
                {
                    if (c == ic) { bad = true; break; }
                }
                sb.Append(bad ? '_' : c);
            }
            string result = sb.ToString().Trim().TrimEnd('.');
            if (result.Length == 0) result = "SF2";
            if (result.Length > 60) result = result.Substring(0, 60).Trim();
            return result;
        }

        public static List<string> BuildAliases(List<string> projectLyrics, string sampleName)
        {
            List<string> result = new List<string>();
            HashSet<string> seen = new HashSet<string>();

            string[] vowels = { "a", "i", "u", "e", "o" };
            string[] consonants = { "", "k", "g", "s", "z", "t", "d", "n", "h", "b", "p",
                                    "m", "y", "r", "w", "f", "v", "j", "ch", "sh", "ts",
                                    "ky", "gy", "ny", "hy", "by", "py", "my", "ry" };
            foreach (string c in consonants)
            {
                foreach (string v in vowels)
                {
                    if (c == "y" && (v == "i" || v == "e")) continue;
                    AddAlias(result, seen, c + v);
                }
            }
            string[] kana =
            {
                "あいうえお", "かきくけこ", "がぎぐげご", "さしすせそ", "ざじずぜぞ",
                "たちつてと", "だぢづでど", "なにぬねの", "はひふへほ", "ばびぶべぼ",
                "ぱぴぷぺぽ", "まみむめも", "やゆよ", "らりるれろ", "わをん",
                "ぁぃぅぇぉ", "ゃゅょ", "っ", "ー", "・", "ん"
            };
            foreach (string row in kana)
            {
                foreach (char ch in row) AddAlias(result, seen, ch.ToString());
            }
            AddAlias(result, seen, "n");
            AddAlias(result, seen, "N");
            AddAlias(result, seen, "-");
            AddAlias(result, seen, "R");
            if (!string.IsNullOrEmpty(sampleName)) AddAlias(result, seen, sampleName.Trim());

            if (projectLyrics != null)
            {
                foreach (string lyric in projectLyrics)
                {
                    AddAlias(result, seen, lyric);
                }
            }
            return result;
        }

        static void AddAlias(List<string> list, HashSet<string> seen, string alias)
        {
            if (string.IsNullOrEmpty(alias)) return;
            string a = alias.Trim();
            if (a.Length == 0) return;
            if (a.IndexOf(',') >= 0) return;
            if (a.IndexOf('=') >= 0) return;
            if (a.IndexOf('\r') >= 0 || a.IndexOf('\n') >= 0) return;
            if (seen.Contains(a)) return;
            seen.Add(a);
            list.Add(a);
        }

        /// <summary>Lyrics of the temporary UST OpenUtau hands to a legacy plugin.</summary>
        public static List<string> ReadProjectLyrics(string path)
        {
            List<string> result = new List<string>();
            if (string.IsNullOrEmpty(path) || !File.Exists(path)) return result;
            string text;
            try
            {
                byte[] bytes = File.ReadAllBytes(path);
                try
                {
                    UTF8Encoding strict = new UTF8Encoding(false, true);
                    text = strict.GetString(bytes);
                }
                catch (DecoderFallbackException)
                {
                    text = Encoding.GetEncoding(932).GetString(bytes);
                }
            }
            catch
            {
                return result;
            }

            string[] lines = text.Replace("\r\n", "\n").Replace('\r', '\n').Split('\n');
            foreach (string raw in lines)
            {
                string line = raw.Trim();
                int eq = line.IndexOf('=');
                if (eq <= 0) continue;
                string key = line.Substring(0, eq).Trim();
                if (!key.Equals("Lyric", StringComparison.OrdinalIgnoreCase)) continue;
                string lyric = line.Substring(eq + 1).Trim();
                if (lyric.Length == 0 || lyric == "R") continue;
                result.Add(lyric);
            }
            return result;
        }
    }
}
