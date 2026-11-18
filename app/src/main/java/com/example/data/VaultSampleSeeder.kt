package com.example.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.pdf.PdfDocument
import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.sin

/**
 * Generates valid, real binary files (PNG, PDF, WAV, TXT) on initial setup
 * so new users can immediately test Gopon Vault's AES-256 encryption and in-app viewers.
 */
object VaultSampleSeeder {

    fun createSampleVaultCardPng(title: String, subtitle: String, accentColor: Int): ByteArray {
        val width = 900
        val height = 560
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(Color.parseColor("#0D081B"), Color.parseColor("#24124D"), accentColor),
                floatArrayOf(0f, 0.65f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), 36f, 36f, bgPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.argb(120, 196, 181, 253)
        }
        canvas.drawRoundRect(RectF(18f, 18f, width - 18f, height - 18f), 28f, 28f, borderPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 48f
            isFakeBoldText = true
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D8CCF8")
            textSize = 28f
        }
        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#22D3EE")
            textSize = 24f
            isFakeBoldText = true
        }

        canvas.drawText("GOPON VAULT • AES-256 ENCRYPTED", 56f, 96f, tagPaint)
        canvas.drawText(title, 56f, 240f, titlePaint)
        canvas.drawText(subtitle, 56f, 300f, subPaint)
        canvas.drawText("Hidden from Phone Gallery • Private Storage", 56f, 470f, subPaint)

        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        bitmap.recycle()
        return out.toByteArray()
    }

    fun createSampleCertificatePdf(ownerName: String): ByteArray {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val bgPaint = Paint().apply { color = Color.parseColor("#0F0A1E") }
        canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A855F7")
            textSize = 24f
            isFakeBoldText = true
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 15f
        }
        val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B3AACB")
            textSize = 13f
        }

        canvas.drawText("GOPON VAULT SECURITY CERTIFICATE", 48f, 88f, headerPaint)
        canvas.drawText("Vault Owner: $ownerName", 48f, 150f, bodyPaint)
        canvas.drawText("Cipher Suite: AES-256-GCM (Android Keystore)", 48f, 185f, bodyPaint)
        canvas.drawText("Key Derivation: PBKDF2-HMAC-SHA256 (100,000 rounds)", 48f, 220f, bodyPaint)
        canvas.drawText("Gallery Isolation: .nomedia enforced inside app-private storage", 48f, 255f, bodyPaint)

        canvas.drawText("Sample Document Preview (Page 1 of 1)", 48f, 330f, mutedPaint)
        canvas.drawText("All imported PDFs, receipts, and ID scans render strictly in-app.", 48f, 356f, mutedPaint)

        doc.finishPage(page)
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    fun createSampleVoiceNoteWav(durationSeconds: Int = 3): ByteArray {
        val sampleRate = 16000
        val numSamples = sampleRate * durationSeconds
        val pcmData = ByteArray(numSamples * 2)
        val freq = 440.0
        for (i in 0 until numSamples) {
            val env = if (i < 1600) i / 1600.0 else if (i > numSamples - 1600) (numSamples - i) / 1600.0 else 1.0
            val sample = (sin(2.0 * PI * freq * i / sampleRate) * 9000.0 * env).toInt().toShort()
            pcmData[i * 2] = (sample.toInt() and 0xFF).toByte()
            pcmData[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }

        val totalDataLen = pcmData.size + 36
        val byteRate = sampleRate * 2
        val header = ByteArray(44)
        fun writeIntLE(offset: Int, v: Int) {
            header[offset] = (v and 0xFF).toByte()
            header[offset + 1] = ((v shr 8) and 0xFF).toByte()
            header[offset + 2] = ((v shr 16) and 0xFF).toByte()
            header[offset + 3] = ((v shr 24) and 0xFF).toByte()
        }
        fun writeShortLE(offset: Int, v: Short) {
            header[offset] = (v.toInt() and 0xFF).toByte()
            header[offset + 1] = ((v.toInt() shr 8) and 0xFF).toByte()
        }
        "RIFF".toByteArray().copyInto(header, 0)
        writeIntLE(4, totalDataLen)
        "WAVE".toByteArray().copyInto(header, 8)
        "fmt ".toByteArray().copyInto(header, 12)
        writeIntLE(16, 16)
        writeShortLE(20, 1)
        writeShortLE(22, 1)
        writeIntLE(24, sampleRate)
        writeIntLE(28, byteRate)
        writeShortLE(32, 2)
        writeShortLE(34, 16)
        "data".toByteArray().copyInto(header, 36)
        writeIntLE(40, pcmData.size)

        return header + pcmData
    }
}
