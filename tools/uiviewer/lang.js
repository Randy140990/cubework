// ---------- languages: the game's words in the language you picked (LANGUAGE, at the start and in OPTIONS) ----------
// T(english) gives the words in the chosen language; {N} is your name and {R} your rival's, put in after.
// Here: every button and key word, the menus, OPTIONS, the title and saves, the battle's commands, and the opening
// with the professor. The rest of the story's lines are English for now.
const LANGCODE = { "English": "en", "Español": "es", "Français": "fr", "Deutsch": "de", "Italiano": "it", "日本語": "ja", "한국어": "ko", "简体中文": "zhs", "繁體中文": "zht" };
const STR = {};
// Each: English, then Spanish, French, German, Italian, Japanese, Korean, Chinese (simplified), Chinese (traditional).
function L(en, es, fr, de, it, ja, ko, zhs, zht) { STR[en] = { es, fr, de, it, ja, ko, zhs, zht }; }
// Buttons and keys.
L("Back", "Atrás", "Retour", "Zurück", "Indietro", "もどる", "돌아가기", "返回", "返回");
L("Choose", "Elegir", "Choisir", "Wählen", "Scegli", "けってい", "결정", "选择", "選擇");
L("Change", "Cambiar", "Changer", "Ändern", "Cambia", "かえる", "변경", "更改", "更改");
L("Open", "Abrir", "Ouvrir", "Öffnen", "Apri", "ひらく", "열기", "打开", "打開");
L("Close", "Cerrar", "Fermer", "Schließen", "Chiudi", "とじる", "닫기", "关闭", "關閉");
L("Next", "Siguiente", "Suivant", "Weiter", "Avanti", "つぎへ", "다음", "下一步", "下一步");
L("Continue", "Continuar", "Continuer", "Weiter", "Continua", "すすむ", "계속", "继续", "繼續");
L("Talk", "Hablar", "Parler", "Reden", "Parla", "はなす", "말걸기", "交谈", "交談");
L("Pick up", "Recoger", "Ramasser", "Aufheben", "Raccogli", "ひろう", "줍기", "捡起", "撿起");
L("Look", "Mirar", "Regarder", "Ansehen", "Guarda", "しらべる", "조사", "调查", "調查");
L("Menu", "Menú", "Menu", "Menü", "Menu", "メニュー", "메뉴", "菜单", "選單");
L("Speed x1", "Velocidad x1", "Vitesse x1", "Tempo x1", "Velocità x1", "はやさ x1", "속도 x1", "速度 x1", "速度 x1");
L("Use", "Usar", "Utiliser", "Benutzen", "Usa", "つかう", "사용", "使用", "使用");
L("Pocket", "Bolsillo", "Poche", "Tasche", "Tasca", "ポケット", "포켓", "口袋", "口袋");
L("Page", "Página", "Page", "Seite", "Pagina", "ページ", "페이지", "页面", "頁面");
L("Entry", "Ficha", "Fiche", "Eintrag", "Scheda", "ずかん", "도감", "图鉴", "圖鑑");
L("Caught only", "Solo capturados", "Capturés seulement", "Nur gefangene", "Solo catturati", "つかまえた だけ", "잡은 것만", "仅已捕获", "僅已捕獲");
L("Zoom in", "Acercar", "Zoomer", "Vergrößern", "Ingrandisci", "ズームイン", "확대", "放大", "放大");
L("Zoom out", "Alejar", "Dézoomer", "Verkleinern", "Riduci", "ズームアウト", "축소", "缩小", "縮小");
L("You", "Tú", "Vous", "Du", "Tu", "じぶん", "나", "自己", "自己");
L("Read it out", "Leer en voz alta", "Lire à voix haute", "Vorlesen", "Leggi ad alta voce", "よみあげる", "읽어 주기", "朗读", "朗讀");
L("Pick", "Elegir", "Prendre", "Nehmen", "Prendi", "えらぶ", "고르기", "挑选", "挑選");
L("Keep and close", "Guardar y cerrar", "Garder et fermer", "Behalten", "Tieni e chiudi", "きめて とじる", "저장하고 닫기", "保存并关闭", "保存並關閉");
L("Other badges", "Otras medallas", "Autres badges", "Andere Orden", "Altre medaglie", "ほかの バッジ", "다른 배지", "其他徽章", "其他徽章");
L("Fly here", "Volar aquí", "Voler ici", "Hierhin fliegen", "Vola qui", "ここへ そらをとぶ", "여기로 날기", "飞到这里", "飛到這裡");
L("YES", "SÍ", "OUI", "JA", "SÌ", "はい", "예", "是", "是");
L("NO", "NO", "NON", "NEIN", "NO", "いいえ", "아니요", "否", "否");
L("CANCEL", "CANCELAR", "ANNULER", "ZURÜCK", "ANNULLA", "やめる", "취소", "取消", "取消");
// The menu.
L("POKéDEX", "POKéDEX", "POKéDEX", "POKéDEX", "POKéDEX", "ずかん", "도감", "图鉴", "圖鑑");
L("POKéMON", "POKéMON", "POKéMON", "POKéMON", "POKéMON", "ポケモン", "포켓몬", "宝可梦", "寶可夢");
L("BAG", "MOCHILA", "SAC", "BEUTEL", "BORSA", "バッグ", "가방", "背包", "背包");
L("MAP", "MAPA", "CARTE", "KARTE", "MAPPA", "マップ", "지도", "地图", "地圖");
L("SAVE", "GUARDAR", "SAUVER", "SICHERN", "SALVA", "レポート", "리포트", "存档", "存檔");
L("CLOTHES", "ROPA", "TENUES", "KLEIDUNG", "VESTITI", "きがえ", "옷", "服装", "服裝");
L("CARD", "FICHA", "DRESSEUR", "PASS", "SCHEDA", "カード", "카드", "卡片", "卡片");
L("WAIT", "ESPERAR", "ATTENDRE", "WARTEN", "ATTENDI", "まつ", "기다리기", "等待", "等待");
L("OPTION", "OPCIONES", "OPTIONS", "OPTIONEN", "OPZIONI", "せってい", "설정", "设置", "設定");
L("EXIT", "SALIR", "QUITTER", "ENDE", "ESCI", "とじる", "닫기", "关闭", "關閉");
L("PARTY", "EQUIPO", "ÉQUIPE", "TEAM", "SQUADRA", "てもち", "파티", "队伍", "隊伍");
L("See how {0} is doing.", "Mira cómo está {0}.", "Vois comment va {0}.", "Sieh nach, wie es {0} geht.", "Guarda come sta {0}.", "{0}の ようすを みる", "{0}의 상태를 본다", "看看{0}的状态", "看看{0}的狀態");
L("Every POKéMON you've seen and caught.", "Todos los POKéMON que has visto y capturado.", "Tous les POKéMON vus et capturés.", "Alle POKéMON, die du gesehen und gefangen hast.", "Tutti i POKéMON che hai visto e catturato.", "みつけた ポケモンと つかまえた ポケモン。", "본 포켓몬과 잡은 포켓몬 전부.", "你见过和捕获的所有宝可梦。", "你見過和捕獲的所有寶可夢。");
L("Your team: check them, switch them, pick who follows you.", "Tu equipo: revísalo, cámbialo y elige quién te sigue.", "Ton équipe : consulte-la, réorganise-la, choisis qui te suit.", "Dein Team: ansehen, tauschen, wählen, wer dir folgt.", "La tua squadra: controllala, cambiala, scegli chi ti segue.", "てもちの ポケモン。いれかえや つれあるきも ここで。", "지닌 포켓몬. 순서를 바꾸고 데리고 다닐 포켓몬을 고르자.", "你的队伍：查看、调换、选择跟着你的宝可梦。", "你的隊伍：查看、調換、選擇跟著你的寶可夢。");
L("Your items, in four pockets.", "Tus objetos, en cuatro bolsillos.", "Tes objets, en quatre poches.", "Deine Items, in vier Taschen.", "I tuoi strumenti, in quattro tasche.", "どうぐは 4つの ポケットに。", "도구는 4개의 포켓에.", "你的道具，分在四个口袋里。", "你的道具，分在四個口袋裡。");
L("The whole region, and where you are in it.", "Toda la región, y dónde estás tú.", "Toute la région, et où tu te trouves.", "Die ganze Region, und wo du gerade bist.", "Tutta la regione, e dove ti trovi.", "ちほう ぜんたいと いまいる ばしょ。", "지방 전체와 지금 있는 곳.", "整个地区，以及你所在的位置。", "整個地區，以及你所在的位置。");
L("Save your adventure so far.", "Guarda tu aventura hasta ahora.", "Sauvegarde ton aventure.", "Sichere dein Abenteuer.", "Salva la tua avventura.", "ここまでの ぼうけんを レポートに かく。", "지금까지의 모험을 기록한다.", "保存目前的冒险。", "保存目前的冒險。");
L("Change your clothes, hat and bag.", "Cámbiate de ropa, gorra y mochila.", "Change de tenue, de chapeau et de sac.", "Wechsle Kleidung, Mütze und Tasche.", "Cambia vestiti, cappello e borsa.", "ふく、ぼうし、バッグを きがえる。", "옷, 모자, 가방을 갈아입는다.", "更换衣服、帽子和背包。", "更換衣服、帽子和背包。");
L("Your trainer card: your name, money and badges.", "Tu ficha de entrenador: nombre, dinero y medallas.", "Ta carte de dresseur : nom, argent et badges.", "Dein Trainerpass: Name, Geld und Orden.", "La tua scheda allenatore: nome, soldi e medaglie.", "トレーナーカード。なまえ、おこづかい、バッジ。", "트레이너 카드. 이름, 돈, 배지.", "训练家卡片：名字、钱和徽章。", "訓練家卡片：名字、錢和徽章。");
L("Let the time pass: morning, midday, evening or night.", "Deja pasar el tiempo: mañana, mediodía, tarde o noche.", "Laisse passer le temps : matin, midi, soir ou nuit.", "Lass Zeit vergehen: Morgen, Mittag, Abend oder Nacht.", "Lascia passare il tempo: mattina, mezzogiorno, sera o notte.", "じかんを すすめる。あさ、ひる、ゆうがた、よる。", "시간을 보낸다. 아침, 낮, 저녁, 밤.", "让时间流逝：早上、中午、傍晚或夜晚。", "讓時間流逝：早上、中午、傍晚或夜晚。");
L("Text speed, music, nights and more.", "Velocidad del texto, música, noches y más.", "Vitesse du texte, musique, nuits et plus.", "Textgeschwindigkeit, Musik, Nächte und mehr.", "Velocità del testo, musica, notti e altro.", "はなしの はやさ、おんがく、よるの あかるさ など。", "글자 속도, 음악, 밤 밝기 등.", "文字速度、音乐、夜晚等。", "文字速度、音樂、夜晚等。");
L("Close the menu.", "Cerrar el menú.", "Fermer le menu.", "Menü schließen.", "Chiudi il menu.", "メニューを とじる。", "메뉴를 닫는다.", "关闭菜单。", "關閉選單。");
// Battle.
L("FIGHT", "LUCHAR", "ATTAQUE", "KAMPF", "LOTTA", "たたかう", "싸운다", "战斗", "戰鬥");
L("RUN", "HUIR", "FUITE", "FLUCHT", "FUGA", "にげる", "도망간다", "逃走", "逃走");
L("What will **{0}** do?", "¿Qué hará **{0}**?", "Que doit faire **{0}** ?", "Was soll **{0}** tun?", "Cosa deve fare **{0}**?", "**{0}**は どうする？", "**{0}**은(는) 무엇을 할까?", "要让**{0}**做什么？", "要讓**{0}**做什麼？");
// Title and saves.
L("Press {0}", "Pulsa {0}", "Appuie sur {0}", "Drücke {0}", "Premi {0}", "{0}を おしてね", "{0}를 누르세요", "请按 {0}", "請按 {0}");
L("A FAN-MADE ADVENTURE", "UNA AVENTURA DE FANS", "UNE AVENTURE DE FANS", "EIN FAN-ABENTEUER", "UN'AVVENTURA DEI FAN", "ファンメイドの ぼうけん", "팬이 만든 모험", "粉丝制作的冒险", "粉絲製作的冒險");
L("PRESS {0}", "PULSA {0}", "APPUIE SUR {0}", "DRÜCKE {0}", "PREMI {0}", "{0}を おしてね", "{0}를 누르세요", "请按 {0}", "請按 {0}");
L("PRESS START", "PULSA START", "APPUIE SUR START", "DRÜCKE START", "PREMI START", "スタートを おしてね", "START를 누르세요", "请按开始键", "請按開始鍵");
L("NEW GAME", "PARTIDA NUEVA", "NOUVELLE PARTIE", "NEUES SPIEL", "NUOVA PARTITA", "さいしょから", "처음부터", "新游戏", "新遊戲");
L("Empty", "Vacía", "Vide", "Leer", "Vuoto", "からっぽ", "비어 있음", "空", "空");
L("SLOT {0}", "RANURA {0}", "EMPLACEMENT {0}", "PLATZ {0}", "SLOT {0}", "スロット {0}", "슬롯 {0}", "存档位 {0}", "存檔位 {0}");
L("OPTIONS", "OPCIONES", "OPTIONS", "OPTIONEN", "OPZIONI", "せってい", "설정", "设置", "設定");
L("DELETE", "BORRAR", "EFFACER", "LÖSCHEN", "CANCELLA", "けす", "삭제", "删除", "刪除");
L("Place", "Lugar", "Lieu", "Ort", "Luogo", "ばしょ", "장소", "地点", "地點");
L("Pokédex", "Pokédex", "Pokédex", "Pokédex", "Pokédex", "ずかん", "도감", "图鉴", "圖鑑");
L("Badges", "Medallas", "Badges", "Orden", "Medaglie", "バッジ", "배지", "徽章", "徽章");
L("Play time", "Tiempo de juego", "Temps de jeu", "Spielzeit", "Tempo di gioco", "プレイじかん", "플레이 시간", "游戏时间", "遊戲時間");
L("Which language would you like to play in?", "¿En qué idioma quieres jugar?", "Dans quelle langue veux-tu jouer ?", "In welcher Sprache möchtest du spielen?", "In che lingua vuoi giocare?", "どの ことばで あそびますか？", "어떤 언어로 플레이하시겠습니까?", "你想用哪种语言玩？", "你想用哪種語言玩？");
L("All the words in the game will be shown in it. You can change it later in OPTIONS.", "Todo el texto del juego aparecerá en este idioma. Puedes cambiarlo luego en OPCIONES.", "Tout le texte du jeu sera dans cette langue. Tu pourras la changer dans OPTIONS.", "Alle Texte im Spiel erscheinen in dieser Sprache. Du kannst sie später in OPTIONEN ändern.", "Tutti i testi del gioco saranno in questa lingua. Potrai cambiarla in OPZIONI.", "ゲームの ことばは すべて この ことばに なります。あとで せってい から かえられます。", "게임의 모든 글이 이 언어로 표시됩니다. 나중에 설정에서 바꿀 수 있습니다.", "游戏中的文字都会以这种语言显示。之后可以在设置中更改。", "遊戲中的文字都會以這種語言顯示。之後可以在設定中更改。");
L("Chosen:", "Elegido:", "Choisie :", "Gewählt:", "Scelta:", "えらんだ ことば：", "선택:", "已选择：", "已選擇：");
// OPTIONS.
L("TEXT SPEED", "VELOCIDAD DEL TEXTO", "VITESSE DU TEXTE", "TEXTTEMPO", "VELOCITÀ TESTO", "はなしの はやさ", "글자 속도", "文字速度", "文字速度");
L("MUSIC", "MÚSICA", "MUSIQUE", "MUSIK", "MUSICA", "おんがく", "음악", "音乐", "音樂");
L("SOUND", "SONIDO", "SON", "TON", "SUONI", "こうかおん", "효과음", "音效", "音效");
L("POKéDEX VOICE", "VOZ DE LA POKéDEX", "VOIX DU POKéDEX", "POKéDEX-STIMME", "VOCE POKéDEX", "ずかんの こえ", "도감 음성", "图鉴语音", "圖鑑語音");
L("NIGHTS", "NOCHES", "NUITS", "NÄCHTE", "NOTTI", "よるの あかるさ", "밤 밝기", "夜晚", "夜晚");
L("WILD POKéMON", "POKéMON SALVAJES", "POKéMON SAUVAGES", "WILDE POKéMON", "POKéMON SELVATICI", "やせいの ポケモン", "야생 포켓몬", "野生宝可梦", "野生寶可夢");
L("BATTLE EFFECTS", "ANIMACIONES", "ANIMATIONS", "KAMPFANIMATION", "ANIMAZIONI", "せんとう アニメ", "전투 애니메이션", "战斗动画", "戰鬥動畫");
L("BATTLE STYLE", "ESTILO DE COMBATE", "STYLE DE COMBAT", "KAMPFSTIL", "STILE LOTTA", "しあいの ルール", "배틀 스타일", "对战方式", "對戰方式");
L("LANGUAGE", "IDIOMA", "LANGUE", "SPRACHE", "LINGUA", "ことば", "언어", "语言", "語言");
L("CONTROLS", "CONTROLES", "COMMANDES", "STEUERUNG", "COMANDI", "そうさ", "조작", "操作", "操作");
for (const [en, ...t] of [["SLOW", "LENTA", "LENT", "LANGSAM", "LENTA", "おそい", "느림", "慢", "慢"], ["MID", "MEDIA", "MOYEN", "MITTEL", "MEDIA", "ふつう", "보통", "中", "中"], ["FAST", "RÁPIDA", "RAPIDE", "SCHNELL", "VELOCE", "はやい", "빠름", "快", "快"],
  ["OFF", "NO", "NON", "AUS", "NO", "オフ", "끔", "关", "關"], ["ON", "SÍ", "OUI", "AN", "SÌ", "オン", "켬", "开", "開"], ["LOW", "BAJO", "BAS", "LEISE", "BASSO", "ちいさい", "작게", "低", "低"], ["HIGH", "ALTO", "HAUT", "LAUT", "ALTO", "おおきい", "크게", "高", "高"],
  ["MAX", "MÁX", "MAX", "MAX", "MAX", "さいだい", "최대", "最大", "最大"], ["DARK", "OSCURAS", "SOMBRES", "DUNKEL", "BUIE", "くらい", "어둡게", "暗", "暗"], ["SOFT", "SUAVES", "DOUCES", "SANFT", "SOFFUSE", "やわらか", "부드럽게", "柔和", "柔和"],
  ["BRIGHT", "CLARAS", "CLAIRES", "HELL", "CHIARE", "あかるい", "밝게", "明亮", "明亮"], ["HIDDEN", "OCULTOS", "CACHÉS", "VERSTECKT", "NASCOSTI", "かくれる", "숨김", "隐藏", "隱藏"], ["SEEN", "VISIBLES", "VISIBLES", "SICHTBAR", "VISIBILI", "みえる", "보임", "可见", "可見"],
  ["SWITCH", "CAMBIO", "CHOIX", "WECHSEL", "CAMBIO", "いれかえ", "교체", "替换", "替換"], ["SET", "FIJO", "DÉFINI", "FEST", "FISSO", "かちぬき", "고정", "固定", "固定"]]) L(en, ...t);
L("How quickly words appear in text boxes.", "Lo rápido que aparece el texto.", "La vitesse d'affichage du texte.", "Wie schnell Texte erscheinen.", "La velocità con cui compare il testo.", "もじが でる はやさ。", "글자가 나오는 속도.", "文字出现的速度。", "文字出現的速度。");
L("How loud the music plays.", "El volumen de la música.", "Le volume de la musique.", "Wie laut die Musik spielt.", "Il volume della musica.", "おんがくの おおきさ。", "음악 크기.", "音乐的音量。", "音樂的音量。");
L("How loud cries, steps and menu sounds are.", "El volumen de gritos, pasos y menús.", "Le volume des cris, pas et menus.", "Wie laut Rufe, Schritte und Menüs sind.", "Il volume di versi, passi e menu.", "なきごえや あしおとの おおきさ。", "울음소리, 발소리, 메뉴 소리 크기.", "叫声、脚步声和菜单音效的音量。", "叫聲、腳步聲和選單音效的音量。");
L("Whether the POKéDEX reads its entries out loud.", "Si la POKéDEX lee las fichas en voz alta.", "Si le POKéDEX lit ses fiches à voix haute.", "Ob der POKéDEX Einträge vorliest.", "Se il POKéDEX legge le schede ad alta voce.", "ずかんが せつめいを よみあげるか。", "도감이 설명을 읽어 줄지.", "图鉴是否朗读条目。", "圖鑑是否朗讀條目。");
L("How dark nights get outside.", "Lo oscuras que son las noches.", "À quel point les nuits sont sombres.", "Wie dunkel Nächte draußen sind.", "Quanto sono buie le notti.", "そとの よるの くらさ。", "바깥의 밤이 얼마나 어두운지.", "户外夜晚的明暗。", "戶外夜晚的明暗。");
L("Whether wild POKéMON show in the grass, or jump out at you.", "Si los POKéMON salvajes se ven en la hierba o te sorprenden.", "Si les POKéMON sauvages sont visibles ou surgissent.", "Ob wilde POKéMON im Gras sichtbar sind oder dich überraschen.", "Se i POKéMON selvatici si vedono nell'erba o sbucano fuori.", "やせいの ポケモンが みえるか、とびだすか。", "야생 포켓몬이 보일지, 갑자기 나올지.", "野生宝可梦是显示在草丛里，还是突然出现。", "野生寶可夢是顯示在草叢裡，還是突然出現。");
L("Whether moves play their animations in battle.", "Si los movimientos muestran animaciones.", "Si les capacités sont animées.", "Ob Attacken animiert werden.", "Se le mosse mostrano animazioni.", "わざの アニメを みるか。", "기술 애니메이션을 볼지.", "招式是否播放动画。", "招式是否播放動畫。");
L("SWITCH asks if you want to change POKéMON when a trainer's sends out the next one.", "CAMBIO te pregunta si quieres cambiar cuando el rival saca otro POKéMON.", "CHOIX te propose de changer quand l'adversaire envoie un POKéMON.", "WECHSEL fragt, ob du tauschen willst, wenn der Gegner ein neues POKéMON schickt.", "CAMBIO ti chiede se vuoi cambiare quando l'avversario manda un altro POKéMON.", "いれかえ：あいてが つぎを だすとき いれかえるか きく。", "교체: 상대가 다음 포켓몬을 낼 때 바꿀지 묻는다.", "替换：对手派出下一只时询问你是否替换。", "替換：對手派出下一隻時詢問你是否替換。");
L("The language the game's words are shown in.", "El idioma del juego.", "La langue du jeu.", "Die Sprache des Spiels.", "La lingua del gioco.", "ゲームの ことば。", "게임의 언어.", "游戏使用的语言。", "遊戲使用的語言。");
L("See every key: what it does in game, and what's turned off.", "Mira cada tecla: qué hace y cuáles están desactivadas.", "Vois chaque touche : son rôle et celles désactivées.", "Jede Taste: was sie tut und was aus ist.", "Ogni tasto: cosa fa e cosa è disattivato.", "キーの やくわりと つかえない キー。", "각 키의 역할과 꺼진 키.", "查看每个按键的作用和已关闭的按键。", "查看每個按鍵的作用和已關閉的按鍵。");
// The opening, with the professor.
L("Ah, there you are! Welcome. I'm **OAK**. Most people round here call me the **POKéMON PROF**.", "¡Ah, ahí estás! Bienvenido. Soy **OAK**. Aquí todos me llaman el **PROFESOR POKéMON**.", "Ah, te voilà ! Bienvenue. Je suis **CHEN**. Par ici, on m'appelle le **PROF POKéMON**.", "Ah, da bist du ja! Willkommen. Ich bin **EICH**. Hier nennt man mich den **POKéMON-PROFESSOR**.", "Ah, eccoti! Benvenuto. Sono **OAK**. Qui tutti mi chiamano il **PROF. POKéMON**.", "おお、きたか！ ようこそ。わしの なまえは **オーキド**。みんなからは **ポケモン はかせ**と よばれて おるよ。", "오, 왔구나! 어서 오렴. 나는 **오박사**란다. 다들 나를 **포켓몬 박사**라고 부르지.", "啊，你来了！欢迎。我是**大木**。大家都叫我**宝可梦博士**。", "啊，你來了！歡迎。我是**大木**。大家都叫我**寶可夢博士**。");
L("This world is full of creatures we call **POKéMON**. You'll find them in the grass, the sea, the caves, even in town.", "Este mundo está lleno de criaturas llamadas **POKéMON**. Viven en la hierba, el mar, las cuevas e incluso en la ciudad.", "Ce monde est peuplé de créatures appelées **POKéMON**. On en trouve dans l'herbe, la mer, les grottes et même en ville.", "Diese Welt ist voller Wesen, die wir **POKéMON** nennen. Du findest sie im Gras, im Meer, in Höhlen, sogar in der Stadt.", "Questo mondo è pieno di creature chiamate **POKéMON**. Le trovi nell'erba, nel mare, nelle grotte e perfino in città.", "この せかいには **ポケモン**と よばれる いきものが いる。くさむら、うみ、どうくつ、まちの なかにも な。", "이 세계에는 **포켓몬**이라 불리는 생물이 산단다. 풀숲, 바다, 동굴, 마을에도 있지.", "这个世界上住着被称为**宝可梦**的生物。草丛、大海、洞窟，甚至城镇里都有。", "這個世界上住著被稱為**寶可夢**的生物。草叢、大海、洞窟，甚至城鎮裡都有。");
L("Some people keep them as friends. Some raise them for battles. Me? I study them.", "Algunos los tienen como amigos. Otros los entrenan para combatir. ¿Yo? Los estudio.", "Certains en font des amis. D'autres les entraînent au combat. Moi ? Je les étudie.", "Manche halten sie als Freunde. Andere trainieren sie für Kämpfe. Ich? Ich erforsche sie.", "Alcuni li tengono come amici. Altri li allenano per lottare. Io? Io li studio.", "ともだちに する ひとも いれば、しょうぶに つかう ひとも いる。わしは けんきゅう しておる。", "친구로 지내는 사람도 있고, 배틀을 위해 키우는 사람도 있지. 나는 연구를 한단다.", "有人把它们当朋友，有人训练它们对战。我嘛，我研究它们。", "有人把牠們當朋友，有人訓練牠們對戰。我嘛，我研究牠們。");
L("But that's enough about me. First, tell me a little about yourself.", "Pero basta de hablar de mí. Primero, háblame un poco de ti.", "Mais assez parlé de moi. D'abord, parle-moi un peu de toi.", "Aber genug von mir. Erzähl mir erst ein wenig über dich.", "Ma basta parlare di me. Prima, dimmi qualcosa di te.", "わしの ことは さておき、まずは きみの ことを おしえて くれ。", "내 얘기는 이쯤 하고, 먼저 너에 대해 알려 주렴.", "我的事就先说到这里。先跟我说说你自己吧。", "我的事就先說到這裡。先跟我說說你自己吧。");
L("What do you look like?", "¿Qué aspecto tienes?", "À quoi ressembles-tu ?", "Wie siehst du aus?", "Che aspetto hai?", "きみの すがたは？", "너는 어떤 모습이니?", "你长什么样子？", "你長什麼樣子？");
L("And what's your name?", "¿Y cómo te llamas?", "Et comment t'appelles-tu ?", "Und wie heißt du?", "E come ti chiami?", "きみの なまえは？", "그리고 네 이름은?", "你叫什么名字？", "你叫什麼名字？");
L("THAT'S ME", "¡ASÍ SOY YO!", "C'EST MOI", "DAS BIN ICH", "SONO IO", "これで いい", "이게 나야", "就是我", "就是我");
L("MIX IT UP", "ALEATORIO", "AU HASARD", "ZUFALL", "A CASO", "おまかせ", "랜덤", "随机", "隨機");
L("NEW NAME", "NOMBRE NUEVO", "NOUVEAU NOM", "NEUER NAME", "NUOVO NOME", "じぶんで きめる", "직접 정하기", "新名字", "新名字");
L("YOUR NAME", "TU NOMBRE", "TON NOM", "DEIN NAME", "IL TUO NOME", "きみの なまえ", "네 이름", "你的名字", "你的名字");
L("YOUR RIVAL'S NAME", "NOMBRE DEL RIVAL", "NOM DU RIVAL", "NAME DES RIVALEN", "NOME DEL RIVALE", "ライバルの なまえ", "라이벌의 이름", "对手的名字", "對手的名字");
L("So you're **{N}**! A fine name.", "¡Así que te llamas **{N}**! Buen nombre.", "Alors tu t'appelles **{N}** ! Joli nom.", "Du heißt also **{N}**! Ein schöner Name.", "Quindi ti chiami **{N}**! Bel nome.", "**{N}**と いうんだな！ いい なまえだ。", "**{N}**(이)라고 하는구나! 좋은 이름이야.", "原来你叫**{N}**！好名字。", "原來你叫**{N}**！好名字。");
L("And this is my grandson. You two have been racing each other since you could walk.", "Y este es mi nieto. Competís desde que aprendisteis a andar.", "Et voici mon petit-fils. Vous faites la course depuis que vous savez marcher.", "Und das ist mein Enkel. Ihr zwei wetteifert, seit ihr laufen könnt.", "E questo è mio nipote. Fate a gara da quando avete imparato a camminare.", "そして これが わしの まご。きみたちは あるけるように なってから ずっと きそいあって おるな。", "그리고 이 아이는 내 손자란다. 너희 둘은 걸음마 때부터 늘 경쟁했지.", "这是我的孙子。你们俩从会走路起就一直在比赛。", "這是我的孫子。你們倆從會走路起就一直在比賽。");
L("Now, what was his name again...?", "Mmm, ¿cómo se llamaba...?", "Euh, comment s'appelle-t-il déjà... ?", "Wie hieß er noch gleich...?", "Ehm, come si chiamava...?", "えーと、なまえは なんだったかな……？", "음, 이름이 뭐였더라……?", "嗯，他叫什么来着……？", "嗯，他叫什麼來著……？");
L("That's right! I remember now. His name is **{R}**!", "¡Eso es! Ya me acuerdo. ¡Se llama **{R}**!", "C'est ça ! Je m'en souviens. Il s'appelle **{R}** !", "Richtig! Jetzt weiß ich es wieder. Er heißt **{R}**!", "Esatto! Ora ricordo. Si chiama **{R}**!", "そうじゃ！ おもいだしたぞ。**{R}**と いう なまえだ！", "그래! 생각났다. 이름은 **{R}**(이)야!", "对了！我想起来了。他叫**{R}**！", "對了！我想起來了。他叫**{R}**！");
L("**{N}**, your own adventure starts today.", "**{N}**, hoy empieza tu aventura.", "**{N}**, ton aventure commence aujourd'hui.", "**{N}**, heute beginnt dein Abenteuer.", "**{N}**, oggi inizia la tua avventura.", "**{N}**！ きみの ぼうけんが いま はじまる。", "**{N}**! 오늘부터 너의 모험이 시작된다.", "**{N}**，你的冒险今天开始了。", "**{N}**，你的冒險今天開始了。");
L("There's a whole region out there, full of POKéMON to meet. I'll be right next door at my lab.", "Ahí fuera te espera una región llena de POKéMON. Estaré en mi laboratorio, justo al lado.", "Toute une région pleine de POKéMON t'attend. Je serai à côté, dans mon labo.", "Da draußen wartet eine ganze Region voller POKéMON. Ich bin gleich nebenan in meinem Labor.", "Là fuori c'è un'intera regione piena di POKéMON. Sarò qui accanto, nel mio laboratorio.", "そとには ポケモンで いっぱいの ちほうが ひろがって おる。わしは となりの けんきゅうじょに おるぞ。", "밖에는 포켓몬으로 가득한 지방이 펼쳐져 있단다. 나는 바로 옆 연구소에 있으마.", "外面是充满宝可梦的广阔地区。我就在隔壁的研究所。", "外面是充滿寶可夢的廣闊地區。我就在隔壁的研究所。");
L("Off you go, then!", "¡Adelante!", "Allez, en route !", "Dann los!", "Allora, vai!", "さあ、いって こい！", "자, 다녀오렴!", "去吧！", "去吧！");
L("PROF. OAK", "PROF. OAK", "PROF. CHEN", "PROF. EICH", "PROF. OAK", "オーキドはかせ", "오박사", "大木博士", "大木博士");
L("MUM", "MAMÁ", "MAMAN", "MAMA", "MAMMA", "ママ", "엄마", "妈妈", "媽媽");
L("NURSE", "ENFERMERA", "INFIRMIÈRE", "SCHWESTER", "INFERMIERA", "ジョーイ", "간호순", "乔伊", "喬伊");
L("CLERK", "DEPENDIENTE", "VENDEUR", "VERKÄUFER", "COMMESSO", "てんいん", "점원", "店员", "店員");
L("BUY", "COMPRAR", "ACHETER", "KAUFEN", "COMPRA", "かいにきた", "사러 왔다", "购买", "購買");
L("SELL", "VENDER", "VENDRE", "VERKAUFEN", "VENDI", "うりにきた", "팔러 왔다", "出售", "出售");
L("SEE YOU", "ADIÓS", "AU REVOIR", "TSCHÜSS", "CIAO", "さようなら", "안녕", "再见", "再見");
// The world.
L("NEXT", "SIGUIENTE", "OBJECTIF", "ZIEL", "PROSSIMO", "つぎ", "다음", "下一步", "下一步");
L("Go to PROF. OAK's lab", "Ve al laboratorio del PROF. OAK", "Va au labo du PROF. CHEN", "Geh zu PROF. EICHs Labor", "Vai al laboratorio del PROF. OAK", "オーキドはかせの けんきゅうじょへ", "오박사의 연구소로 가자", "去大木博士的研究所", "去大木博士的研究所");
L("Game saved", "Partida guardada", "Partie sauvegardée", "Spiel gesichert", "Partita salvata", "レポートを かいた", "저장했습니다", "已存档", "已存檔");

function T(s, ...a) {
  const c = LANGCODE[(typeof G !== "undefined" && G.lang) || "English"] || "en";
  let r = String(s);
  if (c !== "en" && STR[r] && STR[r][c]) r = STR[r][c];
  if (a.length) r = r.replace(/\{(\d)\}/g, (_, i) => a[+i]);
  if (typeof G !== "undefined") r = r.replace(/\{N\}/g, G.name).replace(/\{R\}/g, G.rival);
  return r;
}
