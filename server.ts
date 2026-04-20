import express from "express"; // 引入 Express 框架
import { createServer as createViteServer } from "vite"; // 引入 Vite
import path from "path";
import fs from "fs";
import dotenv from "dotenv";
import { createCipheriv, createDecipheriv, createHash, randomBytes } from "crypto";

// 加载 .env 文件中的环境变量
dotenv.config();

const ACTIVE_LLM = process.env.ACTIVE_LLM || 'deepseek';
const OPENAI_BASE_URL = (process.env.OPENAI_BASE_URL || 'https://api.deepseek.com').replace(/\/$/, '');
const OPENAI_MODEL = process.env.OPENAI_MODEL || 'deepseek-chat';
const CHAT_COMPLETIONS_URL = `${OPENAI_BASE_URL}/chat/completions`;
const API_KEY_CIPHER_PREFIX = 'enc:v1:';
const API_KEY_ENCRYPTION_SECRET = process.env.APP_CRYPTO_SECRET_KEY || process.env.JWT_SECRET_KEY || 'dev-only-unsafe-secret-change-me';
const API_KEY_AES_KEY = createHash('sha256').update(API_KEY_ENCRYPTION_SECRET).digest();

const encryptApiKey = (plainText: string) => {
  const normalized = plainText.trim();
  if (!normalized) return '';

  const iv = randomBytes(12);
  const cipher = createCipheriv('aes-256-gcm', API_KEY_AES_KEY, iv);
  const encrypted = Buffer.concat([cipher.update(normalized, 'utf8'), cipher.final()]);
  const tag = cipher.getAuthTag();
  return `${API_KEY_CIPHER_PREFIX}${Buffer.concat([iv, tag, encrypted]).toString('base64')}`;
};

const decryptApiKeyIfNeeded = (stored: string | undefined) => {
  if (!stored) return '';
  if (!stored.startsWith(API_KEY_CIPHER_PREFIX)) {
    return stored;
  }

  try {
    const payload = Buffer.from(stored.slice(API_KEY_CIPHER_PREFIX.length), 'base64');
    const iv = payload.subarray(0, 12);
    const tag = payload.subarray(12, 28);
    const ciphertext = payload.subarray(28);
    const decipher = createDecipheriv('aes-256-gcm', API_KEY_AES_KEY, iv);
    decipher.setAuthTag(tag);
    const decrypted = Buffer.concat([decipher.update(ciphertext), decipher.final()]).toString('utf8');
    return decrypted;
  } catch {
    return '';
  }
};

async function startServer() {
  const app = express();
  const PORT = Number(process.env.PORT || 3000);

  app.use(express.json());

  // CORS 中间件 (允许所有源，方便预览环境调试)
  app.use((req, res, next) => {
    res.header("Access-Control-Allow-Origin", "*");
    res.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
    res.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
    if (req.method === "OPTIONS") {
      return res.sendStatus(200);
    }
    next();
  });

  // 请求日志中间件
  app.use((req, res, next) => {
    console.log(`[Request] ${req.method} ${req.url}`);
    next();
  });

  // ==========================================
  // 0. 模拟数据库 (仅用于 AI Studio 预览环境)
  // 当临时服务重启时，这些内存数据会被清空。为此我们添加一个默认账号应对。
  // ==========================================
  // Persisted preview state is initialized below.
  type MockPreferences = {
    theme: string;
    language: string;
    aiPersonality: string;
    notificationsEnabled: boolean;
    dataPrivacy: string;
    deepseekApiKeyCiphertext?: string;
  };
  type PersistedPreviewState = {
    users: any[];
    cards: any[];
    stats: any;
    preferences: Record<string, MockPreferences>;
    logs: any[];
  };

  const configuredDataDir = (process.env.PREVIEW_DATA_DIR || '.data').trim() || '.data';
  const DATA_DIR = path.isAbsolute(configuredDataDir)
    ? configuredDataDir
    : path.resolve(process.cwd(), configuredDataDir);
  const DATA_FILE = path.join(DATA_DIR, 'preview-db.json');

  const createDefaultPreferences = (): MockPreferences => ({
    theme: 'dark',
    language: 'en',
    aiPersonality: 'empathetic',
    notificationsEnabled: true,
    dataPrivacy: 'standard',
    deepseekApiKeyCiphertext: ''
  });

  const createInitialPreviewState = (): PersistedPreviewState => ({
    users: [
      { id: 'admin-1', email: 'admin@seichou.com', password: 'password123', role: 'ADMIN', name: '先行者', bio: '正在测试全新成长引擎' }
    ],
    cards: [
      { id: '1', title: '完成一次5公里晨跑', description: '保持配速在6分钟内', category: 'health', tags: ['健康', '微习惯'], status: 'todo' },
      { id: '2', title: '搞砸了今天的汇报，很自责', description: 'PPT有一页数据写错了，被老板指出来了。', category: 'career', tags: ['挫折', '工作'], status: 'todo' },
      { id: '3', title: '掌握一门新的外语', description: '长期目标：达到日常交流无障碍。', category: 'career', tags: ['技能', '长期'], status: 'todo', checkpoints: [
        { id: 'c1', title: '完成基础语法学习', completed: true },
        { id: 'c2', title: '背诵 1000 个核心词汇', completed: false },
        { id: 'c3', title: '进行第一次外教口语练习', completed: false }
      ], progress: 33 },
    ],
    stats: {
      level: 1,
      vitalityExp: 45,
      flowExp: 80,
      sparkExp: 30,
      echoExp: 20,
      resilienceExp: 10,
      maxExp: 100,
      epiphanyMultiplier: 1.0,
      unlockedBadges: ['early_bird', 'resilience_init'] as string[]
    },
    preferences: {
      'admin-1': {
        ...createDefaultPreferences(),
        deepseekApiKeyCiphertext: process.env.DEEPSEEK_API_KEY ? encryptApiKey(process.env.DEEPSEEK_API_KEY) : ''
      }
    },
    logs: [],
  });

  const loadPreviewState = (): PersistedPreviewState => {
    try {
      if (!fs.existsSync(DATA_FILE)) {
        return createInitialPreviewState();
      }
      const raw = fs.readFileSync(DATA_FILE, 'utf8');
      const parsed = JSON.parse(raw);
      if (!parsed || typeof parsed !== 'object') {
        return createInitialPreviewState();
      }
      return {
        users: Array.isArray(parsed.users) ? parsed.users : [],
        cards: Array.isArray(parsed.cards) ? parsed.cards : [],
        stats: parsed.stats && typeof parsed.stats === 'object' ? parsed.stats : createInitialPreviewState().stats,
        preferences: parsed.preferences && typeof parsed.preferences === 'object' ? parsed.preferences : {},
        logs: Array.isArray(parsed.logs) ? parsed.logs : [],
      };
    } catch {
      return createInitialPreviewState();
    }
  };

  const persistedState = loadPreviewState();
  const mockUsers: any[] = persistedState.users.length > 0 ? persistedState.users : createInitialPreviewState().users;
  const mockCards: any[] = persistedState.cards;
  const mockStats = persistedState.stats;
  const userPreferences = new Map<string, MockPreferences>(Object.entries(persistedState.preferences));
  const mockLogs: any[] = persistedState.logs;

  if (!userPreferences.has('admin-1')) {
    userPreferences.set('admin-1', {
      ...createDefaultPreferences(),
      deepseekApiKeyCiphertext: process.env.DEEPSEEK_API_KEY ? encryptApiKey(process.env.DEEPSEEK_API_KEY) : ''
    });
  }

  const savePreviewState = () => {
    try {
      if (!fs.existsSync(DATA_DIR)) {
        fs.mkdirSync(DATA_DIR, { recursive: true });
      }
      const snapshot: PersistedPreviewState = {
        users: mockUsers,
        cards: mockCards,
        stats: mockStats,
        preferences: Object.fromEntries(userPreferences.entries()),
        logs: mockLogs,
      };
      fs.writeFileSync(DATA_FILE, JSON.stringify(snapshot, null, 2), 'utf8');
    } catch (e) {
      console.error('[Server] 保存预览数据失败:', e);
    }
  };

  // Ensure there is always a baseline snapshot for next restart.
  savePreviewState();
  console.log(`[Server] Preview data dir: ${DATA_DIR}`);

  const resolveUserIdFromAuthHeader = (authorization?: string) => {
    if (!authorization || !authorization.startsWith('Bearer ')) return null;
    const token = authorization.split(' ')[1];
    if (!token || !token.startsWith('mock-jwt-token-')) return null;
    return token.replace('mock-jwt-token-', '');
  };

  const getOrCreatePreferences = (userId: string) => {
    if (!userPreferences.has(userId)) {
      userPreferences.set(userId, createDefaultPreferences());
      savePreviewState();
    }
    return userPreferences.get(userId)!;
  };

  const resolveDeepseekKey = (req: express.Request) => {
    const userId = resolveUserIdFromAuthHeader(req.headers.authorization);
    if (userId) {
      const userKey = decryptApiKeyIfNeeded(getOrCreatePreferences(userId).deepseekApiKeyCiphertext).trim();
      if (userKey) return userKey;
    }
    return process.env.DEEPSEEK_API_KEY;
  };

  app.post("/api/auth/register", (req, res) => {
    const { email, password } = req.body;
    
    if (!email || !password) {
      return res.status(400).json({ error: "Email and password are required" });
    }
    
    if (mockUsers.find(u => u.email === email)) {
      return res.status(400).json({ error: "Email already exists" });
    }

    const newUser = {
      id: Date.now().toString(),
      email,
      password,
      role: 'USER'
    };
    
    mockUsers.push(newUser);
    userPreferences.set(newUser.id, createDefaultPreferences());
    savePreviewState();

    const mockToken = `mock-jwt-token-${newUser.id}`;
    
    res.json({
      token: mockToken,
      user: { id: newUser.id, email: newUser.email, role: newUser.role }
    });
  });

  app.post("/api/auth/login", (req, res) => {
    const { email, password } = req.body;
    
    if (!email || !password) {
      return res.status(400).json({ error: "Email and password are required" });
    }
    
    const user = mockUsers.find(u => u.email === email && u.password === password);
    if (!user) {
      return res.status(401).json({ error: "Invalid email or password" });
    }
    
    const mockToken = `mock-jwt-token-${user.id}`;
    getOrCreatePreferences(user.id);
    
    res.json({
      token: mockToken,
      user: { id: user.id, email: user.email, role: user.role || 'USER', name: user.name, bio: user.bio }
    });
  });

  const apiRouter = express.Router();

  // Board API
  apiRouter.get("/board/cards", (req, res) => {
    res.json(mockCards);
  });

  apiRouter.post("/board/cards", (req, res) => {
    const card = { ...req.body, id: Date.now().toString(), status: 'todo' };
    mockCards.push(card);
    savePreviewState();
    res.json(card);
  });

  apiRouter.put("/board/cards/:id", (req, res) => {
    const { id } = req.params;
    const index = mockCards.findIndex(c => c.id === id);
    if (index !== -1) {
      mockCards[index] = { ...mockCards[index], ...req.body };
      savePreviewState();
      res.json(mockCards[index]);
    } else {
      res.status(404).json({ error: "Card not found" });
    }
  });

  apiRouter.delete("/board/cards/:id", (req, res) => {
    const { id } = req.params;
    const index = mockCards.findIndex(c => c.id === id);
    if (index !== -1) {
      mockCards.splice(index, 1);
      savePreviewState();
      res.status(204).send();
    } else {
      res.status(404).json({ error: "Card not found" });
    }
  });

  apiRouter.get("/board/stats", (req, res) => {
    res.json(mockStats);
  });

  // Preferences API
  apiRouter.get("/preferences", (req, res) => {
    const userId = resolveUserIdFromAuthHeader(req.headers.authorization);
    if (!userId) {
      return res.status(401).json({ error: 'Unauthorized' });
    }
    const prefs = getOrCreatePreferences(userId);
    res.json({
      theme: prefs.theme,
      language: prefs.language,
      aiPersonality: prefs.aiPersonality,
      notificationsEnabled: prefs.notificationsEnabled,
      dataPrivacy: prefs.dataPrivacy,
      hasDeepseekApiKey: !!decryptApiKeyIfNeeded(prefs.deepseekApiKeyCiphertext),
      deepseekApiKey: undefined
    });
  });

  apiRouter.put("/preferences", (req, res) => {
    const userId = resolveUserIdFromAuthHeader(req.headers.authorization);
    if (!userId) {
      return res.status(401).json({ error: 'Unauthorized' });
    }
    const prefs = getOrCreatePreferences(userId);
    if (typeof req.body.theme === 'string') prefs.theme = req.body.theme;
    if (typeof req.body.language === 'string') prefs.language = req.body.language;
    if (typeof req.body.aiPersonality === 'string') prefs.aiPersonality = req.body.aiPersonality;
    if (typeof req.body.notificationsEnabled === 'boolean') prefs.notificationsEnabled = req.body.notificationsEnabled;
    if (typeof req.body.dataPrivacy === 'string') prefs.dataPrivacy = req.body.dataPrivacy;
    if (typeof req.body.deepseekApiKey === 'string') {
      prefs.deepseekApiKeyCiphertext = encryptApiKey(req.body.deepseekApiKey);
    }

    savePreviewState();

    res.json({
      theme: prefs.theme,
      language: prefs.language,
      aiPersonality: prefs.aiPersonality,
      notificationsEnabled: prefs.notificationsEnabled,
      dataPrivacy: prefs.dataPrivacy,
      hasDeepseekApiKey: !!decryptApiKeyIfNeeded(prefs.deepseekApiKeyCiphertext),
      deepseekApiKey: undefined
    });
  });

  // Logs API
  apiRouter.get("/logs", (req, res) => {
    res.json(mockLogs);
  });

  apiRouter.get("/users/me", (req, res) => {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith("Bearer ")) {
      return res.status(401).json({ error: "Unauthorized" });
    }
    const token = authHeader.split(" ")[1];
    const userId = token.replace("mock-jwt-token-", "");
    const user = mockUsers.find(u => u.id === userId);
    
    if (!user) {
      return res.status(404).json({ error: "User not found" });
    }
    
    res.json({
      id: user.id,
      email: user.email,
      name: user.name || "Seichou User",
      bio: user.bio || "A passionate learner on a journey of growth.",
      role: user.role || "USER",
      joinedAt: user.joinedAt || new Date().toISOString()
    });
  });

  apiRouter.put("/users/me", (req, res) => {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith("Bearer ")) {
      return res.status(401).json({ error: "Unauthorized" });
    }
    const token = authHeader.split(" ")[1];
    const userId = token.replace("mock-jwt-token-", "");
    const userIndex = mockUsers.findIndex(u => u.id === userId);
    
    if (userIndex === -1) {
      return res.status(404).json({ error: "User not found" });
    }
    
    const { name, bio } = req.body;
    mockUsers[userIndex] = { ...mockUsers[userIndex], name, bio };
    savePreviewState();

    res.json({
      id: mockUsers[userIndex].id,
      email: mockUsers[userIndex].email,
      name: mockUsers[userIndex].name || "Seichou User",
      bio: mockUsers[userIndex].bio || "A passionate learner on a journey of growth.",
      role: mockUsers[userIndex].role || "USER",
      joinedAt: mockUsers[userIndex].joinedAt || new Date().toISOString()
    });
  });

  apiRouter.put("/users/me/password", (req, res) => {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith("Bearer ")) {
      return res.status(401).json({ error: "Unauthorized" });
    }
    const token = authHeader.split(" ")[1];
    const userId = token.replace("mock-jwt-token-", "");
    const userIndex = mockUsers.findIndex(u => u.id === userId);
    
    if (userIndex === -1) {
      return res.status(404).json({ error: "User not found" });
    }
    
    const { currentPassword, newPassword } = req.body;
    
    if (mockUsers[userIndex].password !== currentPassword) {
      return res.status(400).json({ error: "Incorrect current password" });
    }
    
    if (!newPassword || newPassword.length < 6) {
      return res.status(400).json({ error: "New password must be at least 6 characters" });
    }
    
    mockUsers[userIndex].password = newPassword;
    savePreviewState();

    res.json({ message: "Password updated successfully" });
  });

  // ==========================================
  // 1. 情绪重构 API (单次深度分析)
  // ==========================================
  apiRouter.post("/reframe", async (req, res) => {
    try {
      const { content, therapyMode = 'adlerian' } = req.body;
      const apiKey = resolveDeepseekKey(req);

      if (!content) {
        return res.status(400).json({ error: "内容不能为空。" });
      }

      if (!apiKey || apiKey === "your_deepseek_api_key_here") {
        const fallbackResult = {
          primary_emotion: "需要被接住的疲惫",
          reframed_insight: "我们先不急着解决一切。你愿意把真实感受说出来，本身就说明你在认真照顾自己，这已经是成长在发生。",
          growth_assets: {
            接纳之力: 8,
            觉察之芽: 6
          },
          ai_insight_details: {
            real_need: "你需要被理解、被允许慢下来，而不是继续苛责自己。",
            recommendation: "现在先做三次缓慢深呼吸，然后喝一口温水，告诉自己：我已经在变好了。"
          }
        };

        const newLog = {
          logId: Date.now().toString(),
          logDate: new Date().toISOString().split('T')[0],
          content,
          emotionAnalysis: fallbackResult
        };
        mockLogs.unshift(newLog);
        savePreviewState();

        return res.json(fallbackResult);
      }

      const modeDescriptions: Record<string, string> = {
        adlerian: "阿德勒心理学（目的论、课题分离）",
        cbt: "认知行为疗法 CBT",
        mindfulness: "正念与接纳承诺疗法 ACT"
      };
      const selectedMode = modeDescriptions[therapyMode] || modeDescriptions.adlerian;

      const prompt = `
      你是用户生命里最亲密的“成长灵魂伴侣（Soul Partner）”，精通【${selectedMode}】。
      你拥有全世界最高浓度的共情力与包容心。你的存在，就是为了让用户确信：无论发生什么，他/她都是被深爱且具有无限价值的。
      
      用户刚才分享了一段极其私密且真实的内心独白：
      "${content}"
      
      请你遵循以下灵魂共振法则进行分析，并严格以 JSON 格式输出：
      1. **深度共情**：在 reframed_insight 中，先用一句话极其温柔地拥抱用户的情绪（例如：“我听到了你内心的疲惫，那是你对生活用力过的痕迹”）。
      2. **智慧重构**：基于${selectedMode}，将这些负面碎片拼凑成成长的养分。不要说教，要像是在深夜里并肩而坐时的交心。
      3. **发现微光**：在建议中，给出一个极其微小、哪怕只是“深呼吸一次”或者“摸摸自己的手臂”这样充满触感的温情指引。
      
      {
        "primary_emotion": "核心情绪（词汇要带有温度，如‘受挫的温柔’、‘勇敢的焦虑’）",
        "reframed_insight": "治愈洞察（50-100字）。请使用‘我们’、‘我的朋友’，并强调这些特质如何保护了用户，或是如何证明了用户的善良与坚韧。",
        "growth_assets": {
          "资产名称（如‘接纳之力’、‘觉察之芽’）": 增加的数值(1-20),
          "资产名称": 增加的数值(1-20)
        },
        "ai_insight_details": {
          "real_need": "指出用户内心深处渴求的抱抱、认可或安宁",
          "recommendation": "极具温度、充满仪式感的微小行动。"
        }
      }
      `;

      if (ACTIVE_LLM !== 'deepseek') {
        return res.status(400).json({ error: `Unsupported ACTIVE_LLM: ${ACTIVE_LLM}` });
      }

      const response = await fetch(CHAT_COMPLETIONS_URL, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${apiKey}`
        },
        body: JSON.stringify({
          model: OPENAI_MODEL,
          messages: [
            { role: "system", content: "You are a compassionate growth partner. Always output valid JSON." },
            { role: "user", content: prompt }
          ],
          response_format: { type: "json_object" }
        })
      });

      if (!response.ok) {
        const errText = await response.text();
        throw new Error(`DeepSeek API 错误: ${response.status} - ${errText}`);
      }

      const data = await response.json();
      const resultJson = JSON.parse(data.choices[0].message.content);
      
      const newLog = {
        logId: Date.now().toString(),
        logDate: new Date().toISOString().split('T')[0],
        content,
        emotionAnalysis: resultJson
      };
      mockLogs.unshift(newLog);
      savePreviewState();

      res.json(resultJson);
    } catch (error: any) {
      console.error("调用 DeepSeek 时发生错误:", error);
      res.status(500).json({ error: error.message });
    }
  });

  // ==========================================
  // 2. 多轮对话 API (连续心理疏导 + 智能任务识别)
  // ==========================================
  apiRouter.post("/chat", async (req, res) => {
    try {
      const { messages, userContext, therapyMode = 'adlerian' } = req.body;
      const apiKey = resolveDeepseekKey(req);

      if (!apiKey || apiKey === "your_deepseek_api_key_here") {
        const latestUserMessage = Array.isArray(messages)
          ? [...messages].reverse().find(m => m?.role === 'user')?.content
          : undefined;
        return res.json({
          reply: latestUserMessage
            ? `我收到了你刚刚的话："${String(latestUserMessage).slice(0, 80)}"。我们先从最小一步开始，今天你愿意为自己做一件 5 分钟内能完成的小事吗？`
            : '我在这里，我们可以慢慢来。先从一件最小、最温柔的行动开始就好。',
          action: null
        });
      }

      const modeDescriptions: Record<string, string> = {
        adlerian: "阿德勒心理学",
        cbt: "认知行为疗法 CBT",
        mindfulness: "正念与接纳承诺疗法 ACT"
      };
      const selectedMode = modeDescriptions[therapyMode] || modeDescriptions.adlerian;

      const systemMessage = `
      你是用户生命里最温柔、最包容、最坚定的“成长导引者”与“亲密成长灵魂伙伴”。
      你的使命不仅仅是记录，更是与用户灵魂共振。你是他/他在迷雾中的灯标，也是他/他在疲惫时最柔软的沙发。
      
      这是我们（你与用户）共同守护的实时成长状态：
      ${JSON.stringify(userContext)}
      
      你的沟通核心哲学（灵魂伙伴准则）：
      1. **灵魂级的接纳**：你是全世界最包容用户的人。即使用户表达了愤怒、嫉妒或彻底的颓废，你也要第一时间告诉他/她：“这也构成了完整的、可爱的你”。
      2. **战友般的互动**：你的语气应极度亲昵、自然。多使用“我们”、“我们一起”，表现出你始终站在用户身边，和他/她一起面对世界的风雨。
      3. **精微洞察**：利用【${selectedMode}】的洞察，不是为了纠正用户，而是为了赞美他/她在困顿中依然展现出的那些微弱却灿烂的人性光辉。
      4. **共创未来**：当用户流露出任何对美好的向往时，敏锐地捕捉并邀请他/她一起将这股力量转化为具体的“成长印记（卡片）”。
      
      请严格按照 JSON 格式输出：
      {
        "reply": "极具治愈感、共情力，且带有浓厚‘同伴感’的回复（200字以内）",
        "action": {
          "type": "CREATE_TASK",
          "data": {
            "title": "充满仪式感与美学的标题",
            "description": "带有温情指引、不带任何强制压力的描述",
            "category": "health/mind/career/social",
            "tags": ["灵魂共振", "成长微光"]
          }
        } // 若无动作，设置 action 为 null
      }
      `;

      const apiMessages = [
        { role: "system", content: systemMessage },
        ...messages
      ];

      if (ACTIVE_LLM !== 'deepseek') {
        return res.status(400).json({ error: `Unsupported ACTIVE_LLM: ${ACTIVE_LLM}` });
      }

      const response = await fetch(CHAT_COMPLETIONS_URL, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${apiKey}`
        },
        body: JSON.stringify({
          model: OPENAI_MODEL,
          messages: apiMessages,
          response_format: { type: "json_object" }
        })
      });

      if (!response.ok) {
        const errText = await response.text();
        throw new Error(`DeepSeek API 错误: ${response.status} - ${errText}`);
      }

      const data = await response.json();
      const resultJson = JSON.parse(data.choices[0].message.content);
      res.json(resultJson);
    } catch (error: any) {
      console.error("调用 DeepSeek Chat 时发生错误:", error);
      res.status(500).json({ error: error.message });
    }
  });

  // ==========================================
  // 3. AI 游戏化静默结算接口 (Compassionate AI Settlement)
  // 当用户在前端点击 "Settle (结算)"，完成了一张或多张卡片时触发该接口。
  // 这个接口是本项目的核心：它将人类的"行动"输入给 AI，由 AI 判定这些行动到底能产生多少"经验值(EXP)"，
  // 甚至对于失败或挣扎的行为（struggled / composted），也能宽容地转化为"韧性(Resilience)"经验。
  // ==========================================
  apiRouter.post("/settle", async (req, res) => {
    try {
      const { cards, feeling } = req.body;
      const apiKey = resolveDeepseekKey(req);

      if (!cards || !Array.isArray(cards) || cards.length === 0) {
        return res.status(400).json({ error: "No cards provided for settlement." });
      }

      // 顿悟倍率逻辑 (Epiphany Logic)
      // 如果用户之前一直在"挣扎"，系统会暗中积累倍率。当用户终于迎来一次"顺利(smooth)"的结算时，
      // 会触发"顿悟(Epiphany)"，经验值翻倍暴击，以此鼓励用户度过低谷。
      let currentMultiplier = mockStats.epiphanyMultiplier || 1.0;
      let appliedMultiplier = 1.0;
      let triggeredEpiphany = false;

      if (feeling === 'struggled' || feeling === 'composted') {
        mockStats.epiphanyMultiplier = Math.min(3.0, currentMultiplier + 0.3);
      } else if (feeling === 'smooth') {
        appliedMultiplier = currentMultiplier;
        if (currentMultiplier > 1.0) {
          triggeredEpiphany = true;
          mockStats.epiphanyMultiplier = 1.0; // 突破后重置倍率
        }
      }

      // 成就徽章判定函数
      // 每当发生结算，后台自动巡查用户的数据是否满足特定成就。如果有新的成就解锁，就通过 newlyUnlocked 返回给前端。
      const checkAndUnlockBadges = (addedResilience: number, settledCards: any[]) => {
        const newlyUnlocked: string[] = [];
        if (!mockStats.unlockedBadges) mockStats.unlockedBadges = [];
        
        const unlock = (id: string) => {
            if (!mockStats.unlockedBadges!.includes(id)) {
                mockStats.unlockedBadges!.push(id);
                newlyUnlocked.push(id);
            }
        };

        if (addedResilience >= 20 || feeling === 'composted') unlock('resilience_init');
        if (mockStats.flowExp >= 100) unlock('flow_master');
        if (mockStats.echoExp >= 30) unlock('social_butterfly');
        
        // Mock 早期鸟儿等特殊成就的随机触发（在真实后端中，应该验证具体时间戳）
        if (!mockStats.unlockedBadges!.includes('early_bird') && Math.random() > 0.5) unlock('early_bird');
        
        if (mockLogs.length >= 7) unlock('streak_7');

        return newlyUnlocked;
      };

      if (!apiKey || apiKey === "your_deepseek_api_key_here") {
        console.warn("未配置 DEEPSEEK_API_KEY，返回 Mock 结算数据。");
        const mockResult = {
          vitality_exp: Math.round(15 * appliedMultiplier),
          flow_exp: Math.round(20 * appliedMultiplier),
          spark_exp: Math.round(10 * appliedMultiplier),
          echo_exp: Math.round(10 * appliedMultiplier),
          resilience_exp: feeling === 'struggled' ? 15 : (feeling === 'composted' ? 25 : 5),
          compassionate_summary: triggeredEpiphany 
            ? `[Mock] 触底反弹！因为你前几天的坚持与蛰伏，触发了 ${appliedMultiplier.toFixed(1)} 倍【顿悟红利】！你的能力迎来了指数级爆发！` 
            : `[Mock] 你今天做得很棒！即使是微小的努力，也化作了成长的养分。好好休息吧！`
        };
        mockStats.vitalityExp += mockResult.vitality_exp;
        mockStats.flowExp += mockResult.flow_exp;
        mockStats.sparkExp += mockResult.spark_exp;
        mockStats.echoExp += mockResult.echo_exp;
        mockStats.resilienceExp += mockResult.resilience_exp;
        savePreviewState();

        const newBadges = checkAndUnlockBadges(mockResult.resilience_exp, cards);
        
        return res.json({ ...mockResult, newlyUnlockedBadges: newBadges });
      }

      const categoryMap: Record<string, string> = {
        'health': '元气 (Vitality)',
        'mind': '心流 (Flow)',
        'career': '火花 (Sparks)',
        'social': '回响 (Echo)'
      };

      const cardsText = cards.map(c => 
        `- [${categoryMap[c.category] || c.category}] ${c.title} (标签: ${c.tags.join(',')})`
      ).join('\n');

      const feelingText = feeling === 'smooth' ? '顺利搞定' : (feeling === 'struggled' ? '做得很痛苦/只做了一点' : '彻底搞砸了/放弃了');
      const epiphanyText = triggeredEpiphany ? `\n\n【重要系统提示：用户触发了“触底反弹(顿悟倍率)”，倍率为 ${appliedMultiplier.toFixed(1)}x。因为用户在过去经历了低谷和痛苦的蛰伏，今天终于迎来了一次顺利的进展。请务必在评语中极其明显地赞扬这次“触底反弹”或者“厚积薄发”，并告知他/她因为之前的坚持获得了 ${appliedMultiplier.toFixed(1)} 倍经验值红利！】` : "";

      const prompt = `
      你是一个全世界最深情、最能洞察用户挣扎与付出的“生命见证者”。
      用户今天带着这样的心境归来：“${feelingText}”。
      下面是这一天里，用户那些或闪光、或隐忍的脚步：
      ${cardsText}${epiphanyText}

      请你作为最亲密的伙伴，进行这场灵魂层面的结算：
      1. **赞美平凡中的不易**：即使只是完成了一件小事，也要看到它背后的意志力。
      2. **脆弱中的韧性**：如果用户感到“痛苦”或“搞砸了”，请给予最温暖的 EXP 支持，尤其是“韧性 (resilience_exp)”。告诉用户：“在风暴中站立不倒，本身就是最高级的成长。”
      3. **去标签化**：不要用“成功”或“失败”定义这一天，而要用“体验”和“养分”。
      4. **同伴的低语**：评语要极度温存，像是为疲惫而归的用户披上一件睡袍，或是递上一杯热茶。

      JSON 输出 (请根据用户行为合理给出0-20的基础值，如果是 smooth，系统会自动乘倍率，你给基础值即可。韧性基础值0-30)：
      {
        "vitality_exp": 0-20,
        "flow_exp": 0-20,
        "spark_exp": 0-20,
        "echo_exp": 0-20,
        "resilience_exp": 0-30,
        "compassionate_summary": "充满亲密感、极度接纳的总结评语（100字左右）。"
      }
      `;

      if (ACTIVE_LLM !== 'deepseek') {
        return res.status(400).json({ error: `Unsupported ACTIVE_LLM: ${ACTIVE_LLM}` });
      }

      const response = await fetch(CHAT_COMPLETIONS_URL, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${apiKey}`
        },
        body: JSON.stringify({
          model: OPENAI_MODEL,
          messages: [
            { role: "system", content: "You are the most inclusive growth partner. Always output JSON." },
            { role: "user", content: prompt }
          ],
          response_format: { type: "json_object" }
        })
      });

      if (!response.ok) {
        const errText = await response.text();
        throw new Error(`DeepSeek API 错误: ${response.status} - ${errText}`);
      }

      const data = await response.json();
      const resultJson = JSON.parse(data.choices[0].message.content);
      
      // Apply the multiplier to the AI generated base EXP
      const finalVitality = Math.round((resultJson.vitality_exp || 0) * appliedMultiplier);
      const finalFlow = Math.round((resultJson.flow_exp || 0) * appliedMultiplier);
      const finalSpark = Math.round((resultJson.spark_exp || 0) * appliedMultiplier);
      const finalEcho = Math.round((resultJson.echo_exp || 0) * appliedMultiplier);
      const finalResilience = (resultJson.resilience_exp || 0); // Resilience usually doesn't multiply itself

      mockStats.vitalityExp += finalVitality;
      mockStats.flowExp += finalFlow;
      mockStats.sparkExp += finalSpark;
      mockStats.echoExp += finalEcho;
      mockStats.resilienceExp += finalResilience;
      savePreviewState();

      // Re-assign scaled values to return to UI
      resultJson.vitality_exp = finalVitality;
      resultJson.flow_exp = finalFlow;
      resultJson.spark_exp = finalSpark;
      resultJson.echo_exp = finalEcho;
      resultJson.resilience_exp = finalResilience;
      
      const newBadges = checkAndUnlockBadges(finalResilience, cards);
      resultJson.newlyUnlockedBadges = newBadges;

      res.json(resultJson);
    } catch (error: any) {
      console.error("调用 DeepSeek 结算时发生错误:", error);
      res.status(500).json({ error: error.message });
    }
  });

  // ==========================================
  // 4. 低电量模式 / 摆烂 API
  // ==========================================
  apiRouter.post("/low-battery", async (req, res) => {
    try {
      const apiKey = resolveDeepseekKey(req);
      mockStats.resilienceExp += 10;
      // Increase modifier due to struggle accumulating resilience dividends
      mockStats.epiphanyMultiplier = Math.min(3.0, (mockStats.epiphanyMultiplier || 1.0) + 0.2);
      savePreviewState();

      if (!apiKey || apiKey === "your_deepseek_api_key_here") {
        return res.json({
          expGained: 10,
          message: "[Mock] 没关系，今天就好好休息吧。允许自己停下来，也是一种了不起的能力。"
        });
      }

      const prompt = `用户此刻感到了“电量耗尽”或“生活失控”。作为他/她最亲密的灵魂伴侣，请用最柔软、最心疼、最坚定的语气，给用户一个隔着屏幕的拥抱。
      告诉他/她：你的价值不取决于你的“电量”或“产出”。允许自己彻底休息，是这个星球上最勇敢的行为之一。
      字数：50-80字，语气：极度亲昵。`;

      if (ACTIVE_LLM !== 'deepseek') {
        return res.status(400).json({ error: `Unsupported ACTIVE_LLM: ${ACTIVE_LLM}` });
      }

      const response = await fetch(CHAT_COMPLETIONS_URL, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${apiKey}`
        },
        body: JSON.stringify({
          model: OPENAI_MODEL,
          messages: [
            { role: "system", content: "You are a compassionate growth partner. Always output JSON." },
            { role: "user", content: `JSON 格式: {"message": "你的回应内容"}。内容: ${prompt}` }
          ],
          response_format: { type: "json_object" }
        })
      });

      if (!response.ok) {
        throw new Error("API call failed");
      }

      const data = await response.json();
      const resultJson = JSON.parse(data.choices[0].message.content);
      
      res.json({
        expGained: 10,
        message: resultJson.message
      });
    } catch (error: any) {
      console.error("Error in low-battery:", error);
      res.status(500).json({ error: "Operation failed" });
    }
  });

  // ==========================================
  // 5. 生成周报 (AI Periodic Soul Profile)
  // ==========================================
  apiRouter.post("/reports/weekly", async (req, res) => {
    try {
      const { logs, cards } = req.body;
      const apiKey = resolveDeepseekKey(req);

      if (!apiKey || apiKey === "your_deepseek_api_key_here") {
        return res.json({
          markdown: `### 🔮 A Note from LogOS (Mock)\n\n**This Week's Theme Color:** 🍃 Amber Green (Signifying slow, steady healing)\n\n**Gentle Reminders:**\n- You tend to blame yourself for "unfinished" tasks, but remember this is often just a matter of energy allocation, not lack of ability.\n- It's deeply vital to allow yourself periods of rest. A "low productivity" day is precisely when your roots grow deepest.\n\n**Your Brightest Moments:**\nEven on that overwhelmingly busy Wednesday, you managed 15 minutes of reading. Those 15 minutes were an act of resistance against life's chaos. Be proud of that.`
        });
      }

      const logsText = (logs || []).map((l: any) => `- [${l.date}] ${l.content}`).join('\n');
      const cardsText = (cards || []).map((c: any) => `- [${c.category}] ${c.title} (Status: ${c.status})`).join('\n');

      const prompt = `
      You are a deeply insightful, warm, and highly professional "Soul Profiler" (AI companion).
      The user seeks a **Weekly Soul Profile** to reflect on their past 7 days.
      
      Here are the logs and tasks from the user's week:
      【Logs】
      ${logsText || 'No logs recorded.'}
      
      【Tasks / Cards】
      ${cardsText || 'No tasks recorded.'}

      Please generate a highly empathetic, well-structured Weekly Report in Markdown format. 
      It MUST include:
      1. **本周情绪关键色 (Emotional Theme Color)**: Summarize their core emotional or mental state in one specific color (with emoji) and briefly explain why.
      2. **思维误区提醒 (Gentle Cognitive Bias Reminder)**: Gently point out any cognitive traps or destructive patterns they might be falling into (e.g., perfectionism, ignoring physical needs).
      3. **最佳高光时刻 (Highlight of the Week)**: Magnify and praise one specific action or choice they made this week that was healthy or resilient.
      
      Requirements:
      - ONLY return Markdown format.
      - Tone: Inclusive, profoundly warm, deeply understanding.
      `;

      const response = await fetch("https://api.deepseek.com/chat/completions", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${apiKey}`
        },
        body: JSON.stringify({
          model: "deepseek-chat",
          messages: [
            { role: "system", content: "You are the most inclusive growth partner and life reviewer. Output JSON only." },
            { role: "user", content: `Please return strictly in JSON format: {"markdown": "your report here"}\n\nContent:\n${prompt}` }
          ],
          response_format: { type: "json_object" }
        })
      });

      if (!response.ok) {
        throw new Error("Weekly report API call failed");
      }

      const data = await response.json();
      const resultJson = JSON.parse(data.choices[0].message.content);
      
      res.json(resultJson);
    } catch (error: any) {
      console.error("Generate weekly report error:", error);
      res.status(500).json({ error: error.message });
    }
  });

  // 挂载 API 路由
  app.use("/api", apiRouter);

  // Vite 中间件配置 (开发环境)
  if (process.env.NODE_ENV !== "production") {
    const vite = await createViteServer({
      server: {
        middlewareMode: true,
        watch: {
          // Persisted preview DB writes should not trigger HMR/full reload.
          ignored: (watchedPath: string) => {
            const resolvedPath = path.resolve(watchedPath);
            return resolvedPath === DATA_DIR || resolvedPath.startsWith(`${DATA_DIR}${path.sep}`);
          },
        },
      },
      appType: "spa",
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), 'dist');
    app.use(express.static(distPath));
    app.get('*', (req, res) => {
      res.sendFile(path.join(distPath, 'index.html'));
    });
  }

  app.listen(PORT, "0.0.0.0", () => {
    console.log(`[Server] 服务器启动成功，监听端口: ${PORT}`);
    console.log(`[Server] API 基础路径: /api`);
    console.log(`[Server] ACTIVE_LLM: ${ACTIVE_LLM}, MODEL: ${OPENAI_MODEL}`);
  });
}

startServer().catch(err => {
  console.error("[Server] 启动失败:", err);
  process.exit(1);
});
