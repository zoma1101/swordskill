using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using System.Windows.Forms;

namespace OpenUtauSf2
{
    static class Program
    {
        [STAThread]
        static void Main(string[] args)
        {
            if (args.Length >= 2 && args[0] == "--selftest")
            {
                Environment.ExitCode = RunSelfTest(args);
                return;
            }
            if (args.Length >= 2 && args[0] == "--info")
            {
                Environment.ExitCode = RunSelfTest(new string[] { "--selftest", args[1], args.Length >= 3 ? args[2] : Path.Combine(Path.GetTempPath(), "OpenUtauSf2Tool") });
                return;
            }

            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);

            string tempUstPath = "";
            string openFile = "";
            if (args.Length > 0)
            {
                string arg = args[0];
                if (File.Exists(arg) && arg.EndsWith(".sf2", StringComparison.OrdinalIgnoreCase))
                {
                    openFile = arg;
                }
                else
                {
                    // OpenUtau passes its temporary UST file to legacy plugins.
                    tempUstPath = arg;
                }
            }

            using (MainForm form = new MainForm(tempUstPath))
            {
                form.InitialFile = openFile;
                Application.Run(form);
            }
        }

        static int RunSelfTest(string[] args)
        {
            string sf2Path = args[1];
            string outDir = args.Length >= 3 ? args[2] : Path.Combine(Path.GetTempPath(), "OpenUtauSf2Tool");
            StringBuilder log = new StringBuilder();
            int exitCode = 0;
            try
            {
                Directory.CreateDirectory(outDir);
                Sf2File sf2 = Sf2File.Load(sf2Path);
                log.AppendLine("File: " + sf2.FilePath);
                log.AppendLine("Bank name: " + sf2.BankName);
                log.AppendLine("Presets: " + sf2.Presets.Count);
                log.AppendLine("Instruments: " + sf2.Instruments.Count);
                log.AppendLine("Samples: " + sf2.Samples.Count);
                log.AppendLine();

                int presetLimit = Math.Min(20, sf2.Presets.Count);
                for (int i = 0; i < presetLimit; i++)
                {
                    Sf2Preset p = sf2.Presets[i];
                    log.AppendLine(string.Format("[{0}] bank {1:000} program {2:000} {3} zones={4}",
                        i, p.Bank, p.Program, p.Name, p.Zones.Count));
                    foreach (Sf2PresetZone pz in p.Zones)
                    {
                        log.AppendLine(string.Format("    pz key {0}-{1} vel {2}-{3} inst={4}",
                            pz.KeyLow, pz.KeyHigh, pz.VelLow, pz.VelHigh,
                            pz.Instrument != null ? pz.Instrument.Name : "(null)"));
                        if (pz.Instrument != null)
                        {
                            foreach (Sf2Zone iz in pz.Instrument.Zones)
                            {
                                log.AppendLine(string.Format("        iz key {0}-{1} vel {2}-{3} root={4} sample={5}",
                                    iz.KeyLow, iz.KeyHigh, iz.VelLow, iz.VelHigh, iz.RootKey,
                                    iz.Sample != null ? iz.Sample.Name : "(null)"));
                            }
                        }
                    }
                }
                log.AppendLine();

                if (sf2.Presets.Count > 0)
                {
                    Sf2Preset preset = sf2.Presets[0];
                    int[] notes = { 48, 60, 72 };
                    foreach (int note in notes)
                    {
                        List<Sf2Layer> layers = sf2.Resolve(preset, note, 100);
                        log.AppendLine(string.Format("Resolve note {0}: {1} layers", note, layers.Count));
                        if (layers.Count > 0)
                        {
                            short[] pcm = Sf2Synth.Render(layers, 44100, 2.0, true);
                            string wav = Path.Combine(outDir, "preview_" + note + ".wav");
                            WavUtil.WriteWav(wav, pcm, 44100, 2);
                            log.AppendLine("  wrote " + wav + " (" + pcm.Length + " shorts)");
                        }
                    }

                    Sf2Zone zone = sf2.PickZone(preset, 60);
                    if (zone != null && zone.Sample != null)
                    {
                        string sampleWav = Path.Combine(outDir, "sample.wav");
                        WavUtil.WriteSampleWav(zone.Sample, sampleWav);
                        log.AppendLine("Wrote sample: " + sampleWav + " rate=" + zone.Sample.SampleRate
                            + " frames=" + zone.Sample.RawFrames);

                        List<string> lyrics = new List<string>();
                        lyrics.Add("ら");
                        lyrics.Add("らら");
                        lyrics.Add("test");
                        string singers = Path.Combine(outDir, "Singers");
                        string vb = VoicebankExport.Export(sf2, preset, zone.Sample, singers, lyrics, zone);
                        log.AppendLine("Exported voicebank: " + vb);
                        log.AppendLine("  oto.ini lines: " + File.ReadAllLines(Path.Combine(vb, "oto.ini")).Length);
                    }
                }
                log.AppendLine();
                log.AppendLine("SELFTEST OK");
            }
            catch (Exception ex)
            {
                exitCode = 1;
                log.AppendLine("SELFTEST FAILED");
                log.AppendLine(ex.ToString());
            }
            try
            {
                Directory.CreateDirectory(outDir);
                File.WriteAllText(Path.Combine(outDir, "selftest.txt"), log.ToString(), new UTF8Encoding(false));
            }
            catch
            {
            }
            return exitCode;
        }
    }
}
