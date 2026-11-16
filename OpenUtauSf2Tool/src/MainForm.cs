using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.Globalization;
using System.IO;
using System.Media;
using System.Text;
using System.Windows.Forms;

namespace OpenUtauSf2
{
    public class MainForm : Form
    {
        sealed class PresetZoneRef
        {
            public Sf2Preset Preset;
            public Sf2PresetZone Zone;
            public PresetZoneRef(Sf2Preset preset, Sf2PresetZone zone) { Preset = preset; Zone = zone; }
        }

        sealed class InstZoneRef
        {
            public Sf2Instrument Instrument;
            public Sf2Zone Zone;
            public InstZoneRef(Sf2Instrument instrument, Sf2Zone zone) { Instrument = instrument; Zone = zone; }
        }

        sealed class PreviewPlayer : IDisposable
        {
            SoundPlayer player;
            MemoryStream stream;

            public void Play(byte[] wav)
            {
                Stop();
                stream = new MemoryStream(wav);
                player = new SoundPlayer(stream);
                player.Load();
                player.Play();
            }

            public void Stop()
            {
                if (player != null)
                {
                    try { player.Stop(); } catch { }
                    try { player.Dispose(); } catch { }
                    player = null;
                }
                if (stream != null)
                {
                    try { stream.Dispose(); } catch { }
                    stream = null;
                }
            }

            public void Dispose()
            {
                Stop();
            }
        }

        const string AppTitle = "OpenUtau SF2 Tool";
        const int PreviewRate = 44100;

        Sf2File sf2;
        readonly PreviewPlayer player = new PreviewPlayer();
        readonly string tempUstPath;
        List<string> projectLyrics = new List<string>();
        string lastFile = "";
        string lastExportDir = "";
        public string InitialFile = "";

        TreeView tree;
        RichTextBox details;
        TextBox searchBox;
        NumericUpDown noteBox;
        NumericUpDown velocityBox;
        Label noteNameLabel;
        Label pathLabel;
        Label selectionLabel;
        ToolStripStatusLabel statusLabel;
        Button playButton;
        Button stopButton;
        Button wavButton;
        Button voicebankButton;
        Button allSamplesButton;

        public MainForm(string tempUstPath)
        {
            this.tempUstPath = tempUstPath;
            projectLyrics = VoicebankExport.ReadProjectLyrics(tempUstPath);
            LoadSettings();
            BuildUi();
            AllowDrop = true;
            DragEnter += Form_DragEnter;
            DragDrop += Form_DragDrop;
            Shown += Form_Shown;
            FormClosing += Form_FormClosing;
            KeyPreview = true;
            KeyDown += Form_KeyDown;
        }

        void BuildUi()
        {
            SuspendLayout();

            Text = AppTitle;
            StartPosition = FormStartPosition.CenterScreen;
            ClientSize = new Size(1120, 700);
            MinimumSize = new Size(900, 560);
            Font = new Font("Yu Gothic UI", 9.0f);

            MenuStrip menu = new MenuStrip();

            ToolStripMenuItem fileMenu = new ToolStripMenuItem("ファイル(&F)");
            ToolStripMenuItem openItem = new ToolStripMenuItem("SoundFont を開く(&O)...", null, delegate { OpenFileDialogShow(); });
            openItem.ShortcutKeys = Keys.Control | Keys.O;
            ToolStripMenuItem exitItem = new ToolStripMenuItem("終了(&X)", null, delegate { Close(); });
            fileMenu.DropDownItems.Add(openItem);
            fileMenu.DropDownItems.Add(new ToolStripSeparator());
            fileMenu.DropDownItems.Add(exitItem);

            ToolStripMenuItem actionMenu = new ToolStripMenuItem("操作(&A)");
            ToolStripMenuItem playItem = new ToolStripMenuItem("試聴(&P)", null, delegate { PlayCurrent(); });
            playItem.ShortcutKeys = Keys.F5;
            ToolStripMenuItem stopItem = new ToolStripMenuItem("停止(&S)", null, delegate { StopPlayback(); });
            stopItem.ShortcutKeys = Keys.None;
            stopItem.ShortcutKeyDisplayString = "Esc";
            ToolStripMenuItem wavItem = new ToolStripMenuItem("選択を WAV 書き出し(&W)...", null, delegate { ExportWav(); });
            ToolStripMenuItem vbItem = new ToolStripMenuItem("ボイスバンク書き出し(&V)...", null, delegate { ExportVoicebank(); });
            ToolStripMenuItem allItem = new ToolStripMenuItem("全サンプルを書き出し(&A)...", null, delegate { ExportAllSamples(); });
            actionMenu.DropDownItems.Add(playItem);
            actionMenu.DropDownItems.Add(stopItem);
            actionMenu.DropDownItems.Add(new ToolStripSeparator());
            actionMenu.DropDownItems.Add(wavItem);
            actionMenu.DropDownItems.Add(vbItem);
            actionMenu.DropDownItems.Add(allItem);

            ToolStripMenuItem helpMenu = new ToolStripMenuItem("ヘルプ(&H)");
            helpMenu.DropDownItems.Add(new ToolStripMenuItem("このツールについて(&A)...", null, delegate { ShowAbout(); }));

            menu.Items.Add(fileMenu);
            menu.Items.Add(actionMenu);
            menu.Items.Add(helpMenu);

            TableLayoutPanel toolPanel = new TableLayoutPanel();
            toolPanel.Dock = DockStyle.Top;
            toolPanel.AutoSize = true;
            toolPanel.AutoSizeMode = AutoSizeMode.GrowAndShrink;
            toolPanel.Padding = new Padding(6, 6, 6, 4);
            toolPanel.ColumnCount = 12;
            toolPanel.RowCount = 1;
            toolPanel.ColumnStyles.Add(new ColumnStyle(SizeType.AutoSize));
            toolPanel.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 100f));
            toolPanel.ColumnStyles.Add(new ColumnStyle(SizeType.AutoSize));
            toolPanel.ColumnStyles.Add(new ColumnStyle(SizeType.AutoSize));
            toolPanel.ColumnStyles.Add(new ColumnStyle(SizeType.AutoSize));
            toolPanel.ColumnStyles.Add(new ColumnStyle(SizeType.AutoSize));
            toolPanel.ColumnStyles.Add(new ColumnStyle(SizeType.AutoSize));
            toolPanel.ColumnStyles.Add(new ColumnStyle(SizeType.AutoSize));
            toolPanel.ColumnStyles.Add(new ColumnStyle(SizeType.AutoSize));
            toolPanel.ColumnStyles.Add(new ColumnStyle(SizeType.AutoSize));
            toolPanel.ColumnStyles.Add(new ColumnStyle(SizeType.AutoSize));
            toolPanel.ColumnStyles.Add(new ColumnStyle(SizeType.AutoSize));

            Button openButton = new Button();
            openButton.Text = "SF2 を開く...";
            openButton.AutoSize = true;
            openButton.UseVisualStyleBackColor = true;
            openButton.Click += delegate { OpenFileDialogShow(); };
            toolPanel.Controls.Add(openButton, 0, 0);

            pathLabel = new Label();
            pathLabel.Text = "（SoundFont が読み込まれていません）";
            pathLabel.AutoEllipsis = true;
            pathLabel.Dock = DockStyle.Fill;
            pathLabel.TextAlign = ContentAlignment.MiddleLeft;
            pathLabel.Padding = new Padding(8, 0, 8, 0);
            toolPanel.Controls.Add(pathLabel, 1, 0);

            Label searchLabel = new Label();
            searchLabel.Text = "検索:";
            searchLabel.AutoSize = true;
            searchLabel.Anchor = AnchorStyles.Left;
            toolPanel.Controls.Add(searchLabel, 2, 0);

            searchBox = new TextBox();
            searchBox.Width = 150;
            searchBox.Anchor = AnchorStyles.Left;
            searchBox.TextChanged += delegate { RebuildTree(); };
            toolPanel.Controls.Add(searchBox, 3, 0);

            Label noteLabel = new Label();
            noteLabel.Text = "音程:";
            noteLabel.AutoSize = true;
            noteLabel.Anchor = AnchorStyles.Left;
            noteLabel.Padding = new Padding(8, 0, 0, 0);
            toolPanel.Controls.Add(noteLabel, 4, 0);

            noteBox = new NumericUpDown();
            noteBox.Minimum = 0;
            noteBox.Maximum = 127;
            noteBox.Value = 60;
            noteBox.Width = 56;
            noteBox.Anchor = AnchorStyles.Left;
            noteBox.ValueChanged += delegate { UpdateNoteName(); };
            toolPanel.Controls.Add(noteBox, 5, 0);

            noteNameLabel = new Label();
            noteNameLabel.Text = "C4";
            noteNameLabel.AutoSize = true;
            noteNameLabel.Anchor = AnchorStyles.Left;
            noteNameLabel.Font = new Font(Font, FontStyle.Bold);
            noteNameLabel.Padding = new Padding(4, 0, 8, 0);
            toolPanel.Controls.Add(noteNameLabel, 6, 0);

            Label velLabel = new Label();
            velLabel.Text = "ベロシティ:";
            velLabel.AutoSize = true;
            velLabel.Anchor = AnchorStyles.Left;
            toolPanel.Controls.Add(velLabel, 7, 0);

            velocityBox = new NumericUpDown();
            velocityBox.Minimum = 1;
            velocityBox.Maximum = 127;
            velocityBox.Value = 100;
            velocityBox.Width = 56;
            velocityBox.Anchor = AnchorStyles.Left;
            toolPanel.Controls.Add(velocityBox, 8, 0);

            playButton = new Button();
            playButton.Text = "▶ 試聴";
            playButton.AutoSize = true;
            playButton.Anchor = AnchorStyles.Left;
            playButton.UseVisualStyleBackColor = true;
            playButton.Padding = new Padding(4, 0, 4, 0);
            playButton.Click += delegate { PlayCurrent(); };
            toolPanel.Controls.Add(playButton, 9, 0);

            stopButton = new Button();
            stopButton.Text = "■ 停止";
            stopButton.AutoSize = true;
            stopButton.Anchor = AnchorStyles.Left;
            stopButton.UseVisualStyleBackColor = true;
            stopButton.Padding = new Padding(4, 0, 4, 0);
            stopButton.Click += delegate { StopPlayback(); };
            toolPanel.Controls.Add(stopButton, 10, 0);

            TableLayoutPanel rightPanel = new TableLayoutPanel();
            rightPanel.Dock = DockStyle.Fill;
            rightPanel.ColumnCount = 1;
            rightPanel.RowCount = 3;
            rightPanel.ColumnStyles.Add(new ColumnStyle(SizeType.Percent, 100f));
            rightPanel.RowStyles.Add(new RowStyle(SizeType.Percent, 100f));
            rightPanel.RowStyles.Add(new RowStyle(SizeType.AutoSize));
            rightPanel.RowStyles.Add(new RowStyle(SizeType.AutoSize));

            GroupBox detailGroup = new GroupBox();
            detailGroup.Text = "詳細";
            detailGroup.Dock = DockStyle.Fill;
            detailGroup.Padding = new Padding(8);
            details = new RichTextBox();
            details.Dock = DockStyle.Fill;
            details.ReadOnly = true;
            details.BorderStyle = BorderStyle.None;
            details.Font = new Font("Consolas", 9.5f);
            details.BackColor = SystemColors.Window;
            details.HideSelection = false;
            detailGroup.Controls.Add(details);
            rightPanel.Controls.Add(detailGroup, 0, 0);

            GroupBox actionGroup = new GroupBox();
            actionGroup.Text = "書き出し";
            actionGroup.Dock = DockStyle.Fill;
            actionGroup.AutoSize = true;
            actionGroup.AutoSizeMode = AutoSizeMode.GrowAndShrink;
            FlowLayoutPanel actionFlow = new FlowLayoutPanel();
            actionFlow.Dock = DockStyle.Fill;
            actionFlow.AutoSize = true;
            actionFlow.WrapContents = true;
            actionFlow.Padding = new Padding(6);

            wavButton = new Button();
            wavButton.Text = "WAV 書き出し...";
            wavButton.AutoSize = true;
            wavButton.UseVisualStyleBackColor = true;
            wavButton.Click += delegate { ExportWav(); };
            actionFlow.Controls.Add(wavButton);

            voicebankButton = new Button();
            voicebankButton.Text = "OpenUtau ボイスバンクとして書き出し...";
            voicebankButton.AutoSize = true;
            voicebankButton.UseVisualStyleBackColor = true;
            voicebankButton.Click += delegate { ExportVoicebank(); };
            actionFlow.Controls.Add(voicebankButton);

            allSamplesButton = new Button();
            allSamplesButton.Text = "全サンプルを WAV 書き出し...";
            allSamplesButton.AutoSize = true;
            allSamplesButton.UseVisualStyleBackColor = true;
            allSamplesButton.Click += delegate { ExportAllSamples(); };
            actionFlow.Controls.Add(allSamplesButton);

            actionGroup.Controls.Add(actionFlow);
            rightPanel.Controls.Add(actionGroup, 0, 1);

            selectionLabel = new Label();
            selectionLabel.Text = "";
            selectionLabel.AutoSize = true;
            selectionLabel.ForeColor = SystemColors.GrayText;
            selectionLabel.Padding = new Padding(4, 2, 4, 4);
            rightPanel.Controls.Add(selectionLabel, 0, 2);

            tree = new TreeView();
            tree.Dock = DockStyle.Fill;
            tree.HideSelection = false;
            tree.ShowLines = true;
            tree.ShowRootLines = true;
            tree.AfterSelect += delegate { ShowSelectedDetails(); };
            tree.BeforeExpand += Tree_BeforeExpand;
            tree.NodeMouseDoubleClick += delegate(object sender, TreeNodeMouseClickEventArgs e) { PlayCurrent(); };

            SplitContainer split = new SplitContainer();
            split.Dock = DockStyle.Fill;
            split.SplitterDistance = 380;
            split.Panel1.Padding = new Padding(6, 0, 3, 6);
            split.Panel2.Padding = new Padding(3, 0, 6, 6);
            split.Panel1.Controls.Add(tree);
            split.Panel2.Controls.Add(rightPanel);

            StatusStrip status = new StatusStrip();
            statusLabel = new ToolStripStatusLabel();
            statusLabel.Text = "SoundFont を開いてください。OpenUtau の Plugins から起動した場合、選択中の歌詞は自動で使われます。";
            statusLabel.Spring = true;
            statusLabel.TextAlign = ContentAlignment.MiddleLeft;
            status.Items.Add(statusLabel);

            Controls.Add(menu);
            Controls.Add(toolPanel);
            Controls.Add(status);
            Controls.Add(split);
            MainMenuStrip = menu;

            ResumeLayout(true);
            UpdateNoteName();
            EmptyState();
        }

        void Form_Shown(object sender, EventArgs e)
        {
            if (!string.IsNullOrEmpty(InitialFile) && File.Exists(InitialFile))
            {
                OpenSf2(InitialFile, false);
            }
            else if (!string.IsNullOrEmpty(lastFile) && File.Exists(lastFile))
            {
                OpenSf2(lastFile, true);
            }
            else
            {
                details.Text = BuildWelcomeText();
            }
        }

        void Form_FormClosing(object sender, FormClosingEventArgs e)
        {
            player.Stop();
            SaveSettings();
        }

        void Form_KeyDown(object sender, KeyEventArgs e)
        {
            if (e.KeyCode == Keys.F5)
            {
                PlayCurrent();
                e.Handled = true;
            }
            else if (e.KeyCode == Keys.Escape)
            {
                StopPlayback();
                e.Handled = true;
            }
        }

        void Form_DragEnter(object sender, DragEventArgs e)
        {
            if (e.Data.GetDataPresent(DataFormats.FileDrop))
            {
                string[] files = (string[])e.Data.GetData(DataFormats.FileDrop);
                if (files != null && files.Length > 0 && files[0].ToLowerInvariant().EndsWith(".sf2"))
                {
                    e.Effect = DragDropEffects.Copy;
                    return;
                }
            }
            e.Effect = DragDropEffects.None;
        }

        void Form_DragDrop(object sender, DragEventArgs e)
        {
            string[] files = (string[])e.Data.GetData(DataFormats.FileDrop);
            if (files != null && files.Length > 0) OpenSf2(files[0], false);
        }

        void EmptyState()
        {
            tree.Nodes.Clear();
            selectionLabel.Text = "";
        }

        string BuildWelcomeText()
        {
            StringBuilder sb = new StringBuilder();
            sb.AppendLine("OpenUtau SF2 Tool");
            sb.AppendLine();
            sb.AppendLine("SoundFont 2 (.sf2) を読み込み、プリセット・インストゥルメント・");
            sb.AppendLine("サンプルを確認して試聴できます。");
            sb.AppendLine();
            sb.AppendLine("できること");
            sb.AppendLine("  ・プリセット / 音域 / ベロシティ別のゾーン閲覧");
            sb.AppendLine("  ・サンプルの試聴（指定した音程にピッチシフト）");
            sb.AppendLine("  ・サンプルや試聴結果の WAV 書き出し");
            sb.AppendLine("  ・OpenUtau の歌手（ボイスバンク）として書き出し");
            sb.AppendLine();
            sb.AppendLine("使い方");
            sb.AppendLine("  1. 上の「SF2 を開く...」か、SF2 ファイルのドラッグ＆ドロップ");
            sb.AppendLine("  2. 左のツリーからプリセットやサンプルを選択");
            sb.AppendLine("  3. 音程を決めて「▶ 試聴」");
            sb.AppendLine();
            sb.AppendLine("OpenUtau のプラグインとして起動した場合、");
            sb.AppendLine("選択中のノートの歌詞がボイスバンクのエイリアスに追加されます。");
            return sb.ToString();
        }

        void OpenFileDialogShow()
        {
            using (OpenFileDialog dialog = new OpenFileDialog())
            {
                dialog.Title = "SoundFont を開く";
                dialog.Filter = "SoundFont 2 (*.sf2)|*.sf2|すべてのファイル (*.*)|*.*";
                if (dialog.ShowDialog(this) == DialogResult.OK)
                {
                    OpenSf2(dialog.FileName, false);
                }
            }
        }

        void OpenSf2(string path, bool quiet)
        {
            try
            {
                Cursor = Cursors.WaitCursor;
                UseWaitCursor = true;
                Sf2File loaded = Sf2File.Load(path);
                sf2 = loaded;
                lastFile = path;
                projectLyrics = VoicebankExport.ReadProjectLyrics(tempUstPath);
                pathLabel.Text = sf2.FilePath;
                Text = AppTitle + " - " + sf2.FileName;
                RebuildTree();
                int samples = sf2.Samples.Count;
                int presets = sf2.Presets.Count;
                int instruments = sf2.Instruments.Count;
                statusLabel.Text = string.Format(
                    CultureInfo.InvariantCulture,
                    "プリセット {0} / インストゥルメント {1} / サンプル {2}　歌詞 {3} 件を読み込みました",
                    presets, instruments, samples, projectLyrics.Count);
                if (tree.Nodes.Count > 0 && tree.Nodes[0].Nodes.Count > 0)
                {
                    tree.SelectedNode = tree.Nodes[0].Nodes[0];
                }
                SaveSettings();
            }
            catch (Exception ex)
            {
                if (!quiet)
                {
                    MessageBox.Show(this, "SoundFont を読み込めませんでした。\r\n\r\n" + ex.Message,
                        AppTitle, MessageBoxButtons.OK, MessageBoxIcon.Error);
                }
                statusLabel.Text = "読み込みに失敗しました: " + ex.Message;
            }
            finally
            {
                UseWaitCursor = false;
                Cursor = Cursors.Default;
            }
        }

        void UpdateNoteName()
        {
            noteNameLabel.Text = Sf2Note.Name((int)noteBox.Value);
        }

        List<Sf2Preset> SortedPresets()
        {
            List<Sf2Preset> list = new List<Sf2Preset>(sf2.Presets);
            list.Sort(delegate(Sf2Preset a, Sf2Preset b)
            {
                int c = a.Bank.CompareTo(b.Bank);
                if (c != 0) return c;
                c = a.Program.CompareTo(b.Program);
                if (c != 0) return c;
                return string.Compare(a.Name, b.Name, StringComparison.CurrentCulture);
            });
            return list;
        }

        void RebuildTree()
        {
            tree.BeginUpdate();
            try
            {
                tree.Nodes.Clear();
                if (sf2 == null)
                {
                    EmptyState();
                    return;
                }
                string filter = searchBox == null ? "" : searchBox.Text.Trim();
                TreeNode root = new TreeNode("プリセット (" + sf2.Presets.Count.ToString(CultureInfo.InvariantCulture) + ")");
                foreach (Sf2Preset preset in SortedPresets())
                {
                    if (filter.Length > 0)
                    {
                        string haystack = preset.DisplayName + " " + preset.Name;
                        if (haystack.IndexOf(filter, StringComparison.CurrentCultureIgnoreCase) < 0) continue;
                    }
                    TreeNode node = new TreeNode(preset.DisplayName);
                    node.Tag = preset;
                    node.Nodes.Add(new TreeNode("読み込み中..."));
                    root.Nodes.Add(node);
                }
                tree.Nodes.Add(root);
                root.Expand();
                if (root.Nodes.Count == 0)
                {
                    TreeNode none = new TreeNode("該当するプリセットがありません");
                    root.Nodes.Add(none);
                }
            }
            finally
            {
                tree.EndUpdate();
            }
        }

        void Tree_BeforeExpand(object sender, TreeViewCancelEventArgs e)
        {
            Sf2Preset preset = e.Node.Tag as Sf2Preset;
            if (preset == null) return;
            if (e.Node.Nodes.Count == 1 && e.Node.Nodes[0].Tag == null)
            {
                e.Node.Nodes.Clear();
                PopulatePreset(e.Node, preset);
            }
        }

        void PopulatePreset(TreeNode node, Sf2Preset preset)
        {
            int zoneIndex = 1;
            foreach (Sf2PresetZone pz in preset.Zones)
            {
                string text = string.Format(
                    CultureInfo.InvariantCulture,
                    "Zone {0}: {1} / vel {2}-{3}",
                    zoneIndex, pz.KeyRangeText, pz.VelLow, pz.VelHigh);
                TreeNode zoneNode = new TreeNode(text);
                zoneNode.Tag = new PresetZoneRef(preset, pz);
                if (pz.Instrument != null)
                {
                    TreeNode instNode = new TreeNode("Instrument: " + pz.Instrument.Name);
                    instNode.Tag = pz.Instrument;
                    foreach (Sf2Zone iz in pz.Instrument.Zones)
                    {
                        string sampleName = iz.Sample != null ? iz.Sample.Name : "(サンプルなし)";
                        TreeNode izNode = new TreeNode(string.Format(
                            CultureInfo.InvariantCulture,
                            "Zone: {0} / vel {1}-{2} → {3}",
                            iz.KeyRangeText, iz.VelLow, iz.VelHigh, sampleName));
                        izNode.Tag = new InstZoneRef(pz.Instrument, iz);
                        if (iz.Sample != null)
                        {
                            TreeNode sampleNode = new TreeNode("Sample: " + iz.Sample.Name);
                            sampleNode.Tag = iz.Sample;
                            izNode.Nodes.Add(sampleNode);
                        }
                        instNode.Nodes.Add(izNode);
                    }
                    zoneNode.Nodes.Add(instNode);
                }
                node.Nodes.Add(zoneNode);
                zoneIndex++;
            }
            if (node.Nodes.Count == 0)
            {
                node.Nodes.Add(new TreeNode("（ゾーンがありません）"));
            }
        }

        void ShowSelectedDetails()
        {
            TreeNode node = tree.SelectedNode;
            if (node == null || sf2 == null)
            {
                selectionLabel.Text = "";
                return;
            }

            object tag = node.Tag;
            Sf2Preset preset = tag as Sf2Preset;
            Sf2Instrument instrument = tag as Sf2Instrument;
            Sf2Sample sample = tag as Sf2Sample;
            PresetZoneRef pzr = tag as PresetZoneRef;
            InstZoneRef izr = tag as InstZoneRef;

            if (preset != null)
            {
                details.Text = DescribePreset(preset);
                selectionLabel.Text = "選択: プリセット " + preset.DisplayName;
            }
            else if (pzr != null)
            {
                details.Text = DescribePresetZone(pzr);
                selectionLabel.Text = "選択: プリセットゾーン / " + (pzr.Zone.Instrument != null ? pzr.Zone.Instrument.Name : "?");
            }
            else if (instrument != null)
            {
                details.Text = DescribeInstrument(instrument);
                selectionLabel.Text = "選択: インストゥルメント " + instrument.Name;
            }
            else if (izr != null)
            {
                details.Text = DescribeInstrumentZone(izr);
                selectionLabel.Text = "選択: インストゥルメントゾーン / " + (izr.Zone.Sample != null ? izr.Zone.Sample.Name : "?");
            }
            else if (sample != null)
            {
                details.Text = DescribeSample(sample);
                selectionLabel.Text = "選択: サンプル " + sample.Name;
            }
            else
            {
                details.Text = DescribeBank();
                selectionLabel.Text = "選択: SoundFont";
            }
        }

        string DescribeBank()
        {
            StringBuilder sb = new StringBuilder();
            sb.AppendLine("SoundFont");
            sb.AppendLine("  " + sf2.FilePath);
            sb.AppendLine();
            foreach (KeyValuePair<string, string> kv in sf2.Info)
            {
                sb.AppendLine("  " + kv.Key + ": " + kv.Value);
            }
            sb.AppendLine();
            sb.AppendLine("  プリセット: " + sf2.Presets.Count);
            sb.AppendLine("  インストゥルメント: " + sf2.Instruments.Count);
            sb.AppendLine("  サンプル: " + sf2.Samples.Count);
            return sb.ToString();
        }

        string DescribePreset(Sf2Preset preset)
        {
            StringBuilder sb = new StringBuilder();
            sb.AppendLine("Preset: " + preset.Name);
            sb.AppendLine("  Bank:    " + preset.Bank.ToString("000", CultureInfo.InvariantCulture));
            sb.AppendLine("  Program: " + preset.Program.ToString("000", CultureInfo.InvariantCulture));
            sb.AppendLine("  Zones:   " + preset.Zones.Count);
            sb.AppendLine();
            HashSet<string> names = new HashSet<string>();
            foreach (Sf2PresetZone pz in preset.Zones)
            {
                if (pz.Instrument != null) names.Add(pz.Instrument.Name);
            }
            if (names.Count > 0)
            {
                sb.AppendLine("使用インストゥルメント:");
                foreach (string name in names) sb.AppendLine("  - " + name);
                sb.AppendLine();
            }
            sb.AppendLine("プリセットジェネレータ:");
            sb.Append(FormatGens(preset.GlobalGens));
            sb.AppendLine();
            sb.AppendLine("ヒント: ツリーを展開してゾーンとサンプルを確認できます。");
            sb.AppendLine("「▶ 試聴」で現在の音程を鳴らします。");
            return sb.ToString();
        }

        string DescribePresetZone(PresetZoneRef pzr)
        {
            Sf2PresetZone zone = pzr.Zone;
            StringBuilder sb = new StringBuilder();
            sb.AppendLine("Preset Zone");
            sb.AppendLine("  キー範囲:  " + zone.KeyRangeText + " (" + zone.KeyLow + "-" + zone.KeyHigh + ")");
            sb.AppendLine("  ベロシティ: " + zone.VelLow + "-" + zone.VelHigh);
            if (zone.Instrument != null)
            {
                sb.AppendLine("  インストゥルメント: " + zone.Instrument.Name);
                sb.AppendLine("  ゾーン数: " + zone.Instrument.Zones.Count);
            }
            sb.AppendLine();
            sb.AppendLine("ジェネレータ（プリセット global + このゾーン）:");
            sb.Append(FormatGens(zone.Gens));
            return sb.ToString();
        }

        string DescribeInstrument(Sf2Instrument instrument)
        {
            StringBuilder sb = new StringBuilder();
            sb.AppendLine("Instrument: " + instrument.Name);
            sb.AppendLine("  ゾーン数: " + instrument.Zones.Count);
            sb.AppendLine();
            sb.AppendLine("グローバルジェネレータ:");
            sb.Append(FormatGens(instrument.GlobalGens));
            sb.AppendLine();
            sb.AppendLine("サンプル:");
            HashSet<string> seen = new HashSet<string>();
            foreach (Sf2Zone zone in instrument.Zones)
            {
                if (zone.Sample == null) continue;
                if (seen.Contains(zone.Sample.Name)) continue;
                seen.Add(zone.Sample.Name);
                sb.AppendLine("  - " + zone.Sample.Name + "  (" + zone.KeyRangeText + ")");
            }
            return sb.ToString();
        }

        string DescribeInstrumentZone(InstZoneRef izr)
        {
            Sf2Zone zone = izr.Zone;
            StringBuilder sb = new StringBuilder();
            sb.AppendLine("Instrument Zone");
            sb.AppendLine("  キー範囲:  " + zone.KeyRangeText + " (" + zone.KeyLow + "-" + zone.KeyHigh + ")");
            sb.AppendLine("  ベロシティ: " + zone.VelLow + "-" + zone.VelHigh);
            sb.AppendLine("  ルートキー: " + Sf2Note.Name(zone.RootKey) + " (" + zone.RootKey + ")");
            int mode = zone.Get(54, 0);
            sb.AppendLine("  ループモード: " + LoopModeText(mode));
            if (zone.Sample != null)
            {
                sb.AppendLine();
                sb.AppendLine("Sample: " + zone.Sample.Name);
                sb.AppendLine("  サンプルレート: " + zone.Sample.SampleRate + " Hz");
                sb.AppendLine("  長さ: " + zone.Sample.LengthSeconds.ToString("0.000", CultureInfo.InvariantCulture) + " 秒");
                sb.AppendLine("  元のルートキー: " + Sf2Note.Name(zone.Sample.OriginalPitch)
                    + " / 補正 " + zone.Sample.PitchCorrection + " cent");
                sb.AppendLine("  ループ: " + zone.Sample.LoopStart + " - " + zone.Sample.LoopEnd);
            }
            sb.AppendLine();
            sb.AppendLine("ジェネレータ（インストゥルメント global + このゾーン）:");
            sb.Append(FormatGens(zone.Gens));
            return sb.ToString();
        }

        string DescribeSample(Sf2Sample sample)
        {
            StringBuilder sb = new StringBuilder();
            sb.AppendLine("Sample: " + sample.Name);
            sb.AppendLine("  インデックス: " + sample.Index);
            sb.AppendLine("  サンプルレート: " + sample.SampleRate + " Hz");
            sb.AppendLine("  フレーム: " + sample.RawFrames + " (" + sample.LengthSeconds.ToString("0.000", CultureInfo.InvariantCulture) + " 秒)");
            sb.AppendLine("  元のルートキー: " + Sf2Note.Name(sample.OriginalPitch)
                + " (" + sample.OriginalPitch + ") / 補正 " + sample.PitchCorrection + " cent");
            sb.AppendLine("  ループ: " + sample.LoopStart + " - " + sample.LoopEnd
                + (sample.LoopEnd > sample.LoopStart ? "" : "（なし）"));
            sb.AppendLine("  ステレオ: " + (sample.IsStereo ? "はい（左チャンネルを使用）" : "いいえ"));
            sb.AppendLine("  リンク: " + sample.SampleLink);
            sb.AppendLine("  smpl 内オフセット: " + sample.Start + " - " + sample.End);
            sb.AppendLine();
            sb.AppendLine("「▶ 試聴」でこのサンプルを単体で再生できます。");
            return sb.ToString();
        }

        static string LoopModeText(int mode)
        {
            switch (mode)
            {
                case 0: return "なし";
                case 1: return "連続ループ";
                case 2: return "未使用 (2)";
                case 3: return "リリースまでループ";
                default: return mode.ToString(CultureInfo.InvariantCulture);
            }
        }

        static readonly Dictionary<int, string> GenNames = new Dictionary<int, string>()
        {
            {0,"startAddrsOffset"}, {1,"endAddrsOffset"}, {2,"startloopAddrsOffset"}, {3,"endloopAddrsOffset"},
            {4,"startAddrsCoarseOffset"}, {5,"modLfoToPitch"}, {6,"vibLfoToPitch"}, {7,"modEnvToPitch"},
            {8,"initialFilterFc"}, {9,"initialFilterQ"}, {10,"modLfoToFilterFc"}, {11,"modEnvToFilterFc"},
            {12,"endAddrsCoarseOffset"}, {13,"modLfoToVolume"}, {14,"unused1"}, {15,"chorusEffectsSend"},
            {16,"reverbEffectsSend"}, {17,"pan"}, {18,"unused2"}, {19,"unused3"}, {20,"unused4"},
            {21,"delayModLFO"}, {22,"freqModLFO"}, {23,"delayVibLFO"}, {24,"freqVibLFO"},
            {25,"delayModEnv"}, {26,"attackModEnv"}, {27,"holdModEnv"}, {28,"decayModEnv"}, {29,"sustainModEnv"},
            {30,"releaseModEnv"}, {31,"keynumToModEnvHold"}, {32,"keynumToModEnvDecay"},
            {33,"delayVolEnv"}, {34,"attackVolEnv"}, {35,"holdVolEnv"}, {36,"decayVolEnv"}, {37,"sustainVolEnv"},
            {38,"releaseVolEnv"}, {39,"keynumToVolEnvHold"}, {40,"keynumToVolEnvDecay"},
            {41,"instrument"}, {42,"reserved1"}, {43,"keyRange"}, {44,"velRange"}, {45,"startloopAddrsCoarseOffset"},
            {46,"keynum"}, {47,"velocity"}, {48,"initialAttenuation"}, {49,"reserved2"}, {50,"endloopAddrsCoarseOffset"},
            {51,"coarseTune"}, {52,"fineTune"}, {53,"sampleID"}, {54,"sampleModes"}, {55,"reserved3"},
            {56,"scaleTuning"}, {57,"exclusiveClass"}, {58,"overridingRootKey"}
        };

        static string FormatGens(Dictionary<int, int> gens)
        {
            StringBuilder sb = new StringBuilder();
            if (gens == null || gens.Count == 0)
            {
                sb.AppendLine("  （なし）");
                return sb.ToString();
            }
            List<int> keys = new List<int>(gens.Keys);
            keys.Sort();
            foreach (int op in keys)
            {
                string name;
                if (!GenNames.TryGetValue(op, out name)) name = "gen" + op.ToString(CultureInfo.InvariantCulture);
                int value = gens[op];
                string valueText;
                if (op == 43 || op == 44)
                {
                    valueText = string.Format(CultureInfo.InvariantCulture, "{0}-{1}", value & 0xFF, (value >> 8) & 0xFF);
                }
                else if (op == 51 || op == 52 || op == 58)
                {
                    valueText = ((short)value).ToString(CultureInfo.InvariantCulture);
                }
                else
                {
                    valueText = value.ToString(CultureInfo.InvariantCulture);
                }
                sb.AppendLine("  " + name.PadRight(26) + " = " + valueText);
            }
            return sb.ToString();
        }

        // ------------------------------------------------------------------ playback

        List<Sf2Layer> BuildLayers(TreeNode node)
        {
            int note = (int)noteBox.Value;
            int velocity = (int)velocityBox.Value;
            List<Sf2Layer> layers = new List<Sf2Layer>();
            if (node == null || sf2 == null) return layers;

            object tag = node.Tag;
            Sf2Preset preset = tag as Sf2Preset;
            Sf2Instrument instrument = tag as Sf2Instrument;
            Sf2Sample sample = tag as Sf2Sample;
            PresetZoneRef pzr = tag as PresetZoneRef;
            InstZoneRef izr = tag as InstZoneRef;

            if (preset != null)
            {
                layers = sf2.Resolve(preset, note, velocity);
                if (layers.Count == 0)
                {
                    Sf2Zone zone = sf2.PickZone(preset, note);
                    Sf2Layer layer = Sf2Synth.CreateLayerFromZone(zone, note);
                    if (layer != null) layers.Add(layer);
                }
            }
            else if (pzr != null)
            {
                if (pzr.Zone.Instrument != null)
                {
                    foreach (Sf2Zone iz in pzr.Zone.Instrument.Zones)
                    {
                        if (!iz.Matches(note, velocity)) continue;
                        Sf2Layer layer = Sf2Synth.CreateLayer(pzr.Zone, iz, iz.Sample, note, velocity);
                        if (layer != null) layers.Add(layer);
                    }
                    if (layers.Count == 0 && pzr.Zone.Instrument.Zones.Count > 0)
                    {
                        Sf2Zone iz = pzr.Zone.Instrument.Zones[0];
                        Sf2Layer layer = Sf2Synth.CreateLayer(pzr.Zone, iz, iz.Sample, note, velocity);
                        if (layer != null) layers.Add(layer);
                    }
                }
            }
            else if (instrument != null)
            {
                foreach (Sf2Zone iz in instrument.Zones)
                {
                    if (!iz.Matches(note, velocity)) continue;
                    Sf2Layer layer = Sf2Synth.CreateLayer(null, iz, iz.Sample, note, velocity);
                    if (layer != null) layers.Add(layer);
                }
                if (layers.Count == 0 && instrument.Zones.Count > 0)
                {
                    Sf2Layer layer = Sf2Synth.CreateLayer(null, instrument.Zones[0], instrument.Zones[0].Sample, note, velocity);
                    if (layer != null) layers.Add(layer);
                }
            }
            else if (izr != null)
            {
                Sf2Layer layer = Sf2Synth.CreateLayerFromZone(izr.Zone, note);
                if (layer != null) layers.Add(layer);
            }
            else if (sample != null)
            {
                Sf2Layer layer = Sf2Synth.CreateLayerFromSample(sample, note);
                if (layer != null) layers.Add(layer);
            }
            else if (sf2.Presets.Count > 0)
            {
                layers = sf2.Resolve(sf2.Presets[0], note, velocity);
            }
            return layers;
        }

        static double PreviewSeconds(List<Sf2Layer> layers)
        {
            double seconds = 0.35;
            foreach (Sf2Layer layer in layers)
            {
                if (layer == null || layer.Sample == null) continue;
                if (layer.Loop)
                {
                    seconds = Math.Max(seconds, 2.2);
                }
                else
                {
                    double step = layer.Ratio * layer.Sample.SampleRate / (double)PreviewRate;
                    double dur = step > 0 ? (layer.End - layer.Start) / step / PreviewRate : 0.5;
                    seconds = Math.Max(seconds, Math.Min(dur + 0.15, 6.0));
                }
            }
            return Math.Max(0.35, Math.Min(8.0, seconds));
        }

        void PlayCurrent()
        {
            try
            {
                if (sf2 == null)
                {
                    statusLabel.Text = "先に SoundFont を開いてください。";
                    return;
                }
                TreeNode node = tree.SelectedNode;
                List<Sf2Layer> layers = BuildLayers(node);
                if (layers.Count == 0)
                {
                    statusLabel.Text = "この選択には再生できるゾーン / サンプルがありません。";
                    return;
                }
                double seconds = PreviewSeconds(layers);
                short[] pcm = Sf2Synth.Render(layers, PreviewRate, seconds, true);
                player.Play(WavUtil.CreateWav(pcm, PreviewRate, 2));
                statusLabel.Text = string.Format(
                    CultureInfo.InvariantCulture,
                    "再生中: {0} で {1:0.00} 秒（{2} レイヤー）",
                    Sf2Note.Name((int)noteBox.Value), seconds, layers.Count);
            }
            catch (Exception ex)
            {
                statusLabel.Text = "試聴に失敗しました: " + ex.Message;
            }
        }

        void StopPlayback()
        {
            player.Stop();
            statusLabel.Text = "停止しました。";
        }

        // ------------------------------------------------------------------ export

        string DefaultExportName()
        {
            TreeNode node = tree.SelectedNode;
            if (node != null)
            {
                Sf2Sample sample = node.Tag as Sf2Sample;
                if (sample != null) return VoicebankExport.SanitizeFileName(sample.Name);
                Sf2Preset preset = node.Tag as Sf2Preset;
                if (preset != null) return VoicebankExport.SanitizeFileName(preset.Name);
                InstZoneRef izr = node.Tag as InstZoneRef;
                if (izr != null && izr.Zone.Sample != null) return VoicebankExport.SanitizeFileName(izr.Zone.Sample.Name);
            }
            return sf2 != null ? VoicebankExport.SanitizeFileName(sf2.FileName) : "sf2";
        }

        void ExportWav()
        {
            if (sf2 == null) return;
            TreeNode node = tree.SelectedNode;
            Sf2Sample sample = node == null ? null : node.Tag as Sf2Sample;
            using (SaveFileDialog dialog = new SaveFileDialog())
            {
                dialog.Title = "WAV 書き出し";
                dialog.Filter = "WAV (*.wav)|*.wav";
                dialog.FileName = DefaultExportName() + ".wav";
                if (dialog.ShowDialog(this) != DialogResult.OK) return;
                try
                {
                    if (sample != null)
                    {
                        WavUtil.WriteSampleWav(sample, dialog.FileName);
                        statusLabel.Text = "サンプルを書き出しました: " + dialog.FileName;
                    }
                    else
                    {
                        List<Sf2Layer> layers = BuildLayers(node);
                        if (layers.Count == 0)
                        {
                            MessageBox.Show(this, "書き出せるサンプルがありません。", AppTitle,
                                MessageBoxButtons.OK, MessageBoxIcon.Information);
                            return;
                        }
                        double seconds = PreviewSeconds(layers);
                        short[] pcm = Sf2Synth.Render(layers, PreviewRate, seconds, true);
                        WavUtil.WriteWav(dialog.FileName, pcm, PreviewRate, 2);
                        statusLabel.Text = "試聴結果を書き出しました: " + dialog.FileName;
                    }
                }
                catch (Exception ex)
                {
                    MessageBox.Show(this, "WAV を書き出せませんでした。\r\n\r\n" + ex.Message, AppTitle,
                        MessageBoxButtons.OK, MessageBoxIcon.Error);
                }
            }
        }

        Sf2Preset FindPreset(TreeNode node)
        {
            while (node != null)
            {
                Sf2Preset preset = node.Tag as Sf2Preset;
                if (preset != null) return preset;
                node = node.Parent;
            }
            return null;
        }

        Sf2Sample FindSample(TreeNode node)
        {
            TreeNode current = node;
            while (current != null)
            {
                Sf2Sample sample = current.Tag as Sf2Sample;
                if (sample != null) return sample;
                InstZoneRef izr = current.Tag as InstZoneRef;
                if (izr != null && izr.Zone.Sample != null) return izr.Zone.Sample;
                PresetZoneRef pzr = current.Tag as PresetZoneRef;
                if (pzr != null && pzr.Zone.Instrument != null && pzr.Zone.Instrument.Zones.Count > 0
                    && pzr.Zone.Instrument.Zones[0].Sample != null)
                {
                    return pzr.Zone.Instrument.Zones[0].Sample;
                }
                current = current.Parent;
            }
            return null;
        }

        static Sf2Zone FindZoneForSample(Sf2Preset preset, Sf2Sample sample)
        {
            if (preset == null || sample == null) return null;
            foreach (Sf2PresetZone pz in preset.Zones)
            {
                if (pz.Instrument == null) continue;
                foreach (Sf2Zone iz in pz.Instrument.Zones)
                {
                    if (iz.Sample == sample) return iz;
                }
            }
            return null;
        }

        void ExportVoicebank()
        {
            if (sf2 == null) return;
            TreeNode node = tree.SelectedNode;
            Sf2Preset preset = FindPreset(node);
            if (preset == null && sf2.Presets.Count > 0) preset = sf2.Presets[0];
            Sf2Sample sample = FindSample(node);
            Sf2Zone exportZone = null;
            for (TreeNode current = node; current != null && exportZone == null; current = current.Parent)
            {
                InstZoneRef izr = current.Tag as InstZoneRef;
                if (izr != null && izr.Zone.Sample != null) exportZone = izr.Zone;
            }
            if (sample == null && preset != null)
            {
                exportZone = sf2.PickZone(preset, (int)noteBox.Value);
                if (exportZone != null) sample = exportZone.Sample;
            }
            if (exportZone == null && sample != null && preset != null)
            {
                exportZone = FindZoneForSample(preset, sample);
            }
            if (sample == null)
            {
                MessageBox.Show(this, "書き出せるサンプルがありません。", AppTitle,
                    MessageBoxButtons.OK, MessageBoxIcon.Information);
                return;
            }

            string initial = lastExportDir;
            if (string.IsNullOrEmpty(initial) || !Directory.Exists(initial)) initial = SingersDir();
            string folder = PickFolder("ボイスバンクの出力先（OpenUtau の Singers フォルダなど）", initial);
            if (folder == null) return;

            try
            {
                UseWaitCursor = true;
                string output = VoicebankExport.Export(sf2, preset, sample, folder, projectLyrics, exportZone);
                lastExportDir = folder;
                SaveSettings();
                statusLabel.Text = "ボイスバンクを書き出しました: " + output;
                DialogResult result = MessageBox.Show(this,
                    "ボイスバンクを書き出しました。\r\n\r\n" + output
                    + "\r\n\r\nOpenUtau を再起動するか歌手一覧を更新すると使えます。\r\nフォルダを開きますか？",
                    AppTitle, MessageBoxButtons.YesNo, MessageBoxIcon.Information);
                if (result == DialogResult.Yes)
                {
                    Process.Start("explorer.exe", "\"" + output + "\"");
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show(this, "ボイスバンクを書き出せませんでした。\r\n\r\n" + ex.Message,
                    AppTitle, MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
            finally
            {
                UseWaitCursor = false;
            }
        }

        void ExportAllSamples()
        {
            if (sf2 == null) return;
            string initial = lastExportDir;
            if (string.IsNullOrEmpty(initial) || !Directory.Exists(initial))
            {
                initial = Path.GetDirectoryName(sf2.FilePath);
            }
            string folder = PickFolder("全サンプルの出力先", initial);
            if (folder == null) return;
            try
            {
                UseWaitCursor = true;
                int count = 0;
                foreach (Sf2Sample sample in sf2.Samples)
                {
                    string name = string.Format(CultureInfo.InvariantCulture, "{0:000}_{1}.wav",
                        sample.Index, VoicebankExport.SanitizeFileName(sample.Name));
                    string path = Path.Combine(folder, name);
                    WavUtil.WriteSampleWav(sample, path);
                    count++;
                }
                lastExportDir = folder;
                SaveSettings();
                statusLabel.Text = count + " 個のサンプルを書き出しました: " + folder;
                MessageBox.Show(this, count + " 個のサンプルを書き出しました。\r\n\r\n" + folder,
                    AppTitle, MessageBoxButtons.OK, MessageBoxIcon.Information);
            }
            catch (Exception ex)
            {
                MessageBox.Show(this, "サンプルを書き出せませんでした。\r\n\r\n" + ex.Message,
                    AppTitle, MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
            finally
            {
                UseWaitCursor = false;
            }
        }

        string PickFolder(string description, string initial)
        {
            using (FolderBrowserDialog dialog = new FolderBrowserDialog())
            {
                dialog.Description = description;
                dialog.ShowNewFolderButton = true;
                if (!string.IsNullOrEmpty(initial) && Directory.Exists(initial))
                {
                    dialog.SelectedPath = initial;
                }
                return dialog.ShowDialog(this) == DialogResult.OK ? dialog.SelectedPath : null;
            }
        }

        static string SingersDir()
        {
            string docs = Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments);
            return Path.Combine(Path.Combine(docs, "OpenUtau"), "Singers");
        }

        void ShowAbout()
        {
            MessageBox.Show(this,
                "OpenUtau SF2 Tool\r\n\r\n"
                + "SoundFont 2 (.sf2) の閲覧・試聴と、OpenUtau 用ボイスバンクへの書き出しを行うツールです。\r\n\r\n"
                + "・SF2 の音声は 16bit PCM として読み込みます。\r\n"
                + "・サンプルは指定した音程にピッチシフトして試聴します。\r\n"
                + "・書き出したボイスバンクは 1 サンプルを歌詞ごとに割り当てた簡易版です。\r\n\r\n"
                + "OpenUtau の Plugins フォルダに置くと、ピアノロールの右クリックメニューから起動できます。",
                AppTitle, MessageBoxButtons.OK, MessageBoxIcon.Information);
        }

        // ------------------------------------------------------------------ settings

        static string SettingsPath
        {
            get
            {
                return Path.Combine(
                    Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "OpenUtauSf2Tool"),
                    "settings.ini");
            }
        }

        void LoadSettings()
        {
            try
            {
                if (!File.Exists(SettingsPath)) return;
                string[] lines = File.ReadAllLines(SettingsPath, Encoding.UTF8);
                foreach (string line in lines)
                {
                    int eq = line.IndexOf('=');
                    if (eq <= 0) continue;
                    string key = line.Substring(0, eq).Trim();
                    string value = line.Substring(eq + 1).Trim();
                    if (key == "LastFile") lastFile = value;
                    else if (key == "LastExportDir") lastExportDir = value;
                }
            }
            catch
            {
            }
        }

        void SaveSettings()
        {
            try
            {
                string dir = Path.GetDirectoryName(SettingsPath);
                Directory.CreateDirectory(dir);
                StringBuilder sb = new StringBuilder();
                sb.AppendLine("LastFile=" + lastFile);
                sb.AppendLine("LastExportDir=" + lastExportDir);
                File.WriteAllText(SettingsPath, sb.ToString(), new UTF8Encoding(false));
            }
            catch
            {
            }
        }
    }
}
