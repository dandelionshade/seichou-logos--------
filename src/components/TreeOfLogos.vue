<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue';
import { useBoardStore } from '@/store/boardStore';
import { Leaf } from 'lucide-vue-next';

const canvasRef = ref<HTMLCanvasElement | null>(null);
const containerRef = ref<HTMLElement | null>(null);
const boardStore = useBoardStore();

let animationFrameId: number;
let time = 0;

// Simple seeded PRNG for stable tree generation
let seed = 42;
function random(localSeed?: number) {
  if (localSeed !== undefined) seed = localSeed;
  let x = Math.sin(seed++) * 10000;
  return x - Math.floor(x);
}

interface Branch {
  x: number;
  y: number;
  len: number;
  angle: number;
  depth: number;
  thickness: number;
  id: number;
}

let branches: Branch[] = [];
let leaves: { x: number, y: number, size: number, phase: number, color: string }[] = [];
let sparks: { x: number, y: number, size: number, phase: number }[] = [];
let echoes: { x: number, size: number, phase: number }[] = [];

// Rebuild the abstract syntax tree of our Logo Tree
const buildTreeData = (width: number, height: number) => {
  const stats = boardStore.stats;
  
  branches = [];
  leaves = [];
  sparks = [];
  echoes = [];
  seed = 42; // reset seed for stable geometry

  // 1. Vitality -> Trunk thickness
  const baseThickness = 1 + Math.min(10, stats.vitalityExp / 20) + (stats.level * 0.5);
  // 2. Level -> Base length
  const baseLen = (height * 0.22) + Math.min(height * 0.1, stats.level * 2);
  
  const maxDepth = 6;
  
  // 3. Flow -> Leaf probability & size
  const flowExp = stats.flowExp;
  const leafProb = Math.min(0.9, 0.2 + (flowExp / 200));
  const baseLeafSize = 3 + Math.min(5, flowExp / 50);

  // 4. Spark -> Fruit probability
  const sparkExp = stats.sparkExp;
  const sparkProb = Math.min(0.5, 0.05 + (sparkExp / 200));

  // 5. Echo -> Moss spread at base
  const echoExp = stats.echoExp;
  const echoSpread = 20 + Math.min(width / 2, echoExp);
  const echoCount = Math.min(100, 10 + Math.floor(echoExp / 2));

  // Generate Echo (Moss)
  const rootX = width / 2;
  const rootY = height * 0.85;
  for (let i = 0; i < echoCount; i++) {
    echoes.push({
      x: rootX + (random() - 0.5) * echoSpread * 2,
      size: 1 + random() * 2.5,
      phase: random() * Math.PI * 2
    });
  }

  // Recursive branch generator
  let idCounter = 0;
  const grow = (x: number, y: number, len: number, angle: number, depth: number, parentId: number = -1) => {
    const endX = x + len * Math.sin(angle);
    const endY = y - len * Math.cos(angle);
    const thickness = baseThickness * (depth / maxDepth);
    
    const branchId = idCounter++;
    const b: Branch & { parentId: number, childIds: number[], swayOffset: number } = { 
      x, y, len, angle, depth, thickness, id: branchId, parentId, childIds: [], swayOffset: random() * Math.PI * 2 
    };
    branches.push(b);

    if (depth > 0) {
      const splitAngle1 = angle - (0.2 + random() * 0.3);
      const splitAngle2 = angle + (0.2 + random() * 0.3);
      const lenDiscount = 0.7 + random() * 0.15;
      
      b.childIds.push(grow(endX, endY, len * lenDiscount, splitAngle1, depth - 1, branchId));
      b.childIds.push(grow(endX, endY, len * lenDiscount, splitAngle2, depth - 1, branchId));
      
      if (random() > 0.7) {
        b.childIds.push(grow(endX, endY, len * 0.5, angle + (random() - 0.5) * 0.2, depth - 1, branchId));
      }
    } else {
      if (random() < leafProb) {
        const clr = random() > 0.5 ? '#88D66C' : '#A3E68C';
        leaves.push({
          x: endX, y: endY, 
          size: baseLeafSize * (0.5 + random() * 0.5),
          phase: random() * Math.PI * 2,
          color: clr,
          parentId: branchId
        });
      }
      if (random() < sparkProb) {
        sparks.push({
          x: endX, y: endY,
          size: 1 + random() * 2,
          phase: random() * Math.PI * 2,
          parentId: branchId
        });
      }
    }
    return branchId;
  };

  grow(rootX, rootY, baseLen, 0, maxDepth, -1);
};

const renderFrame = (ctx: CanvasRenderingContext2D, width: number, height: number) => {
  ctx.clearRect(0, 0, width, height);
  time += 0.015;

  const rootY = height * 0.85;

  // We need to calculate realistic current positions recursively so branches don't disconnect
  const currentPositions = new Map<number, { x: number, y: number, angle: number }>();
  
  // They are ordered from root to leaves (due to recursion pushing after evaluate, wait no, 
  // root is pushed first? Yes, because we push before recursing.)
  for (const b of branches as any[]) {
    let startX = b.x;
    let startY = b.y;
    let currentAngle = b.angle;
    
    if (b.parentId !== -1) {
       const parentPos = currentPositions.get(b.parentId);
       if (parentPos) {
         startX = parentPos.x;
         startY = parentPos.y;
         // inherit parent's rotation sway slightly
         currentAngle += parentPos.angle * 0.5; 
       }
    }
    
    // Calculate swaying based on depth and time
    const sway = Math.sin(time + b.swayOffset) * ((7 - b.depth) * 0.015);
    currentAngle += sway;
    
    const endX = startX + b.len * Math.sin(currentAngle);
    const endY = startY - b.len * Math.cos(currentAngle);
    
    currentPositions.set(b.id, { x: endX, y: endY, angle: sway });

    ctx.beginPath();
    ctx.moveTo(startX, startY);
    ctx.lineWidth = Math.max(0.5, b.thickness);
    ctx.strokeStyle = 'rgba(212, 175, 55, 0.4)'; // Golden brown trunk
    ctx.lineCap = 'round';
    ctx.lineTo(endX, endY);
    ctx.stroke();
  }

  // Draw Leaves (Flow)
  for (const leaf of leaves as any[]) {
    let lx = leaf.x; let ly = leaf.y;
    const parentPos = currentPositions.get(leaf.parentId);
    if (parentPos) { lx = parentPos.x; ly = parentPos.y; }
    
    const swayX = Math.sin(time * 1.2 + leaf.phase) * 3;
    const swayY = Math.cos(time * 0.8 + leaf.phase) * 2;
    ctx.beginPath();
    ctx.arc(lx + swayX, ly + swayY, leaf.size, 0, Math.PI * 2);
    ctx.fillStyle = leaf.color;
    ctx.globalAlpha = 0.8;
    ctx.fill();
    ctx.globalAlpha = 1.0;
  }

  // Draw Sparks (Career/Fireflies)
  for (const spark of sparks as any[]) {
    let sx = spark.x; let sy = spark.y;
    const parentPos = currentPositions.get(spark.parentId);
    if (parentPos) { sx = parentPos.x; sy = parentPos.y; }

    const floatY = Math.sin(time * 2 + spark.phase) * 5;
    const glow = (Math.sin(time * 3 + spark.phase) + 1) / 2; // 0 to 1
    
    ctx.beginPath();
    ctx.arc(sx, sy - floatY, spark.size + glow, 0, Math.PI * 2);
    ctx.fillStyle = '#FFB84C'; // Golden spark
    ctx.shadowBlur = 10 * glow;
    ctx.shadowColor = '#FFB84C';
    ctx.fill();
    ctx.shadowBlur = 0;
  }

  // Draw Echo (Social/Moss at root)
  // Drawn last to cover the base
  ctx.fillStyle = 'rgba(136, 214, 108, 0.3)';
  for (const echo of echoes) {
    const pulse = Math.sin(time + echo.phase) * 0.5;
    ctx.beginPath();
    ctx.arc(echo.x, rootY + pulse, echo.size, 0, Math.PI); // Half circles
    ctx.fill();
  }
};

const animate = () => {
  if (canvasRef.value) {
    const ctx = canvasRef.value.getContext('2d');
    if (ctx) {
      renderFrame(ctx, canvasRef.value.width, canvasRef.value.height);
    }
  }
  animationFrameId = requestAnimationFrame(animate);
};

const resizeAndBuild = () => {
  if (containerRef.value && canvasRef.value) {
    const { clientWidth, clientHeight } = containerRef.value;
    canvasRef.value.width = clientWidth;
    canvasRef.value.height = clientHeight;
    buildTreeData(clientWidth, clientHeight);
  }
};

onMounted(() => {
  resizeAndBuild();
  window.addEventListener('resize', resizeAndBuild);
  animate();
});

onUnmounted(() => {
  window.removeEventListener('resize', resizeAndBuild);
  cancelAnimationFrame(animationFrameId);
});

// Reactively rebuild tree when stats change significantly
// We deep watch stats to regrow the tree elements logically
watch(() => boardStore.stats, () => {
  resizeAndBuild();
}, { deep: true });

const props = defineProps<{
  interactive?: boolean
}>();

const emit = defineEmits<{
  (e: 'click'): void
}>();

const handleClick = () => {
  if (props.interactive) {
    emit('click');
  }
};
</script>

<template>
  <div 
    ref="containerRef" 
    class="relative w-full h-full flex flex-col items-center justify-center overflow-hidden bg-card-bg/20 rounded-2xl group transition-all"
    :class="{ 'cursor-pointer hover:bg-card-bg/40': interactive }"
    @click="handleClick"
  >
    <canvas ref="canvasRef" class="w-full h-full object-contain mix-blend-screen opacity-90"></canvas>
    
    <!-- Overlaid Info - Only show when NOT interactive (i.e. sidebar) or explicitly needed -->
    <div v-if="!interactive" class="absolute bottom-4 left-4 right-4 pointer-events-none transition-opacity duration-500 opacity-60 group-hover:opacity-100">
      <div class="flex items-center gap-2 mb-1">
        <div class="p-1 rounded bg-bg/50 backdrop-blur-sm border border-border/50 text-accent-glow">
          <Leaf :size="12" />
        </div>
        <h3 class="font-serif text-[11px] text-ink uppercase tracking-widest font-bold drop-shadow-md">逻各斯之树 (Tree of Logos)</h3>
      </div>
      <p class="text-[9px] text-ink-dim/80 leading-relaxed font-medium">树高映照阅历(Level) • 树干扎根元气(Vitality)<br/>叶落知晓心流(Flow) • 花果结于星火(Spark) • 地衣生于回响(Echo)</p>
    </div>
  </div>
</template>
