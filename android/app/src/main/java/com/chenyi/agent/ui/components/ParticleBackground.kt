package com.chenyi.agent.ui.components

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import com.chenyi.agent.ui.theme.AccentCyan
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * 粒子数据类
 * 存储单个粒子的位置、速度、大小和透明度信息
 */
data class Particle(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val opacity: Float
) {
    /**
     * 更新粒子位置
     * @param width 画布宽度
     * @param height 画布高度
     */
    fun update(width: Float, height: Float) {
        x += vx
        y += vy

        // 边界循环处理
        if (x < 0) x = width
        if (x > width) x = 0f
        if (y < 0) y = height
        if (y > height) y = 0f
    }
}

/**
 * 粒子背景 Composable
 * 实现全屏粒子动画效果，粒子随机运动并在距离小于阈值时绘制连线
 *
 * @param particleCount 粒子数量，默认80个
 * @param particleColor 粒子颜色，默认使用主题的AccentCyan
 * @param connectionDistance 连线距离阈值，默认100像素
 * @param minParticleSize 最小粒子大小，默认0.5像素
 * @param maxParticleSize 最大粒子大小，默认2.5像素
 * @param minParticleSpeed 最小粒子速度，默认0.5像素/帧
 * @param maxParticleSpeed 最大粒子速度，默认0.5像素/帧
 * @param minOpacity 最小透明度，默认0.1
 * @param maxOpacity 最大透明度，默认0.6
 * @param connectionOpacity 连线基础透明度，默认0.08
 * @param modifier Modifier修饰符
 */
@Composable
fun ParticleBackground(
    particleCount: Int = 80,
    particleColor: Color = AccentCyan,
    connectionDistance: Float = 100f,
    minParticleSize: Float = 0.5f,
    maxParticleSize: Float = 2.5f,
    minParticleSpeed: Float = 0.5f,
    maxParticleSpeed: Float = 0.5f,
    minOpacity: Float = 0.1f,
    maxOpacity: Float = 0.6f,
    connectionOpacity: Float = 0.08f,
    modifier: Modifier = Modifier
) {
    val particles = remember { mutableStateListOf<Particle>() }
    var canvasSize = remember { IntSize.Zero }

    // 初始化粒子
    LaunchedEffect(particleCount) {
        // 等待画布尺寸初始化
        if (canvasSize.width <= 0 || canvasSize.height <= 0) {
            canvasSize = IntSize(1080, 1920) // 默认尺寸
        }
        
        particles.clear()
        repeat(particleCount) {
            particles.add(
                createParticle(
                    width = canvasSize.width.toFloat(),
                    height = canvasSize.height.toFloat(),
                    minSize = minParticleSize,
                    maxSize = maxParticleSize,
                    minSpeed = minParticleSpeed,
                    maxSpeed = maxParticleSpeed,
                    minOpacity = minOpacity,
                    maxOpacity = maxOpacity
                )
            )
        }
    }

    // 动画循环
    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameNanos { _ ->
                val width = canvasSize.width.toFloat().coerceAtLeast(1f)
                val height = canvasSize.height.toFloat().coerceAtLeast(1f)
                
                particles.forEach { particle ->
                    particle.update(width, height)
                }
            }
        }
    }

    Canvas(
        modifier = modifier
    ) {
        // 更新画布尺寸
        val currentSize = IntSize(size.width.toInt(), size.height.toInt())
        if (canvasSize != currentSize) {
            canvasSize = currentSize
        }

        val width = size.width
        val height = size.height

        // 绘制连线（先绘制连线，再绘制粒子，使粒子在上层）
        particles.forEachIndexed { i, particleA ->
            particles.drop(i + 1).forEach { particleB ->
                val dx = particleA.x - particleB.x
                val dy = particleA.y - particleB.y
                val distance = sqrt(dx.pow(2) + dy.pow(2))
                
                if (distance < connectionDistance && distance > 0) {
                    val alpha = connectionOpacity * (1f - distance / connectionDistance)
                    drawLine(
                        color = particleColor,
                        start = Offset(particleA.x, particleA.y),
                        end = Offset(particleB.x, particleB.y),
                        strokeWidth = 0.5f,
                        alpha = alpha.coerceIn(0f, 1f)
                    )
                }
            }
        }

        // 绘制粒子
        particles.forEach { particle ->
            drawCircle(
                color = particleColor,
                radius = particle.size,
                center = Offset(particle.x, particle.y),
                alpha = particle.opacity
            )
        }
    }
}

/**
 * 创建单个粒子
 */
private fun createParticle(
    width: Float,
    height: Float,
    minSize: Float,
    maxSize: Float,
    minSpeed: Float,
    maxSpeed: Float,
    minOpacity: Float,
    maxOpacity: Float
): Particle {
    val random = Random.Default
    
    // 随机位置
    val x = random.nextFloat() * width
    val y = random.nextFloat() * height
    
    // 随机速度（可正可负）
    val speedRange = maxSpeed - minSpeed
    val vx = (random.nextFloat() * 2 - 1) * (minSpeed + random.nextFloat() * speedRange)
    val vy = (random.nextFloat() * 2 - 1) * (minSpeed + random.nextFloat() * speedRange)
    
    // 随机大小
    val size = minSize + random.nextFloat() * (maxSize - minSize)
    
    // 随机透明度
    val opacity = minOpacity + random.nextFloat() * (maxOpacity - minOpacity)
    
    return Particle(
        x = x,
        y = y,
        vx = vx,
        vy = vy,
        size = size,
        opacity = opacity
    )
}

/**
 * 简化版粒子背景
 * 使用默认参数快速创建粒子背景
 *
 * @param modifier Modifier修饰符
 */
@Composable
fun ParticleBackgroundSimple(
    modifier: Modifier = Modifier
) {
    ParticleBackground(
        particleCount = 80,
        particleColor = AccentCyan,
        connectionDistance = 100f,
        modifier = modifier
    )
}

/**
 * 密集粒子背景
 * 适用于需要更密集粒子效果的场景
 *
 * @param modifier Modifier修饰符
 */
@Composable
fun ParticleBackgroundDense(
    modifier: Modifier = Modifier
) {
    ParticleBackground(
        particleCount = 150,
        particleColor = AccentCyan,
        connectionDistance = 80f,
        minParticleSize = 0.3f,
        maxParticleSize = 1.5f,
        modifier = modifier
    )
}

/**
 * 自定义颜色粒子背景
 *
 * @param color 粒子颜色
 * @param modifier Modifier修饰符
 */
@Composable
fun ParticleBackgroundColored(
    color: Color,
    modifier: Modifier = Modifier
) {
    ParticleBackground(
        particleCount = 80,
        particleColor = color,
        connectionDistance = 100f,
        modifier = modifier
    )
}
