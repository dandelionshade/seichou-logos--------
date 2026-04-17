// src/utils/audio.ts

//这是一个不需要外部音频文件（免载入时间、免流量）的音频生成工具类。
// 它使用了 HTML5 底层的 Web Audio API，通过控制计算机发声波形（Sine / Sawtooth / Triangle）
// 来合成带有极客感和游戏化体验的界面音效。

class AudioSystem {
  // 核心的音频环境对象
  private ctx: AudioContext | null = null;
  // 是否允许发声的全局开关
  private enabled: boolean = true;

  // 初始化 AudioContext (必须由用户交互如点击时才能真正启动，这是浏览器的安全策略)
  private init() {
    if (!this.ctx) {
      const AudioContextClass = window.AudioContext || (window as any).webkitAudioContext;
      if (AudioContextClass) {
        this.ctx = new AudioContextClass();
      }
    }
    // 如果被挂起，则唤醒它
    if (this.ctx && this.ctx.state === 'suspended') {
      this.ctx.resume().catch(() => {});
    }
  }

  // 供外部组件（如 MainLayout 中的静音按钮）使用的开关控制
  public setEnabled(val: boolean) {
    this.enabled = val;
  }

  // 一个底层发音函数包裹器，决定了发出什么“频率”和“波长”的声音
  private playTone(freq: number, type: OscillatorType, duration: number, vol = 0.1) {
    if (!this.enabled) return;
    try {
      this.init();
      if (!this.ctx) return;
      const t = this.ctx.currentTime;
      // 振荡器 (发声源)
      const osc = this.ctx.createOscillator();
      // 增益节点 (音量控制器)
      const gain = this.ctx.createGain();
      
      osc.type = type; // 波形类型：sine, square, sawtooth, triangle
      osc.frequency.setValueAtTime(freq, t);
      
      gain.gain.setValueAtTime(vol, t); // 设置初始音量
      gain.gain.exponentialRampToValueAtTime(0.001, t + duration); // 音量逐渐衰减，产生“敲击”余音感
      
      // 连接路线: 发声源 -> 音量控制器 -> 电脑扬声器(destination)
      osc.connect(gain);
      gain.connect(this.ctx.destination);
      
      osc.start(t); // 开始响
      osc.stop(t + duration); // 指定时间后停止
    } catch (e) {
      // 捕获可能不支持 AudioContext 的旧浏览器报错，优雅降级（不发声）
    }
  }

  // A subtle, low-pitch click for mundane interactions (e.g. crossing a checkpoint)
  click() {
    if (!this.enabled) return;
    try {
      this.init();
      if (!this.ctx) return;
      const t = this.ctx.currentTime;
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(400, t);
      osc.frequency.exponentialRampToValueAtTime(100, t + 0.05);
      gain.gain.setValueAtTime(0.05, t);
      gain.gain.exponentialRampToValueAtTime(0.001, t + 0.05);
      osc.connect(gain);
      gain.connect(this.ctx.destination);
      osc.start(t);
      osc.stop(t + 0.05);
    } catch (e) {}
  }

  // A warm ascending chime for completing a card or successful save
  success() {
    if (!this.enabled) return;
    try {
      this.init();
      if (!this.ctx) return;
      [523.25, 659.25, 783.99].forEach((freq, i) => { // C5, E5, G5
        setTimeout(() => {
          this.playTone(freq, 'sine', 0.4, 0.05);
        }, i * 80);
      });
    } catch (e) {}
  }

  // A glittering arpeggio for unlocking a badge or settling the day
  unlock() {
    if (!this.enabled) return;
    try {
      this.init();
      if (!this.ctx) return;
      const notes = [440, 554.37, 659.25, 880, 1108.73, 1318.51]; // A major arp
      notes.forEach((freq, i) => {
        setTimeout(() => {
          this.playTone(freq, 'triangle', 0.6, 0.04);
        }, i * 60);
      });
    } catch (e) {}
  }

  // A soft error buzz
  error() {
    if (!this.enabled) return;
    try {
      this.init();
      if (!this.ctx) return;
      const t = this.ctx.currentTime;
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();
      osc.type = 'sawtooth';
      osc.frequency.setValueAtTime(150, t);
      osc.frequency.exponentialRampToValueAtTime(80, t + 0.2);
      gain.gain.setValueAtTime(0.05, t);
      gain.gain.exponentialRampToValueAtTime(0.001, t + 0.2);
      osc.connect(gain);
      gain.connect(this.ctx.destination);
      osc.start(t);
      osc.stop(t + 0.2);
    } catch(e) {}
  }
}

export const soundFx = new AudioSystem();
