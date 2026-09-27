package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.data.model.CompanyProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * مدير الملف التعريفي للشركة والشعار المخصص
 * يحفظ الإعدادات في SharedPreferences ويخزن الشعار المخصص في مجلد التطبيق الداخلي
 */
class CompanyProfileManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "company_profile_prefs"
        private const val KEY_NAME = "company_name"
        private const val KEY_PHONE = "company_phone"
        private const val KEY_TAX_NUMBER = "company_tax_number"
        private const val KEY_ADDRESS = "company_address"
        private const val KEY_LOGO_URI = "company_logo_uri"
        private const val KEY_LOGO_PRESET = "company_logo_preset"
        private const val KEY_FOOTER_NOTE = "company_footer_note"
        private const val LOGO_FILE_NAME = "company_receipt_logo.png"
    }

    /**
     * استرجاع الملف التعريفي المحفوظ حالياً
     */
    fun getCompanyProfile(): CompanyProfile {
        val savedLogoPath = prefs.getString(KEY_LOGO_URI, null)
        val validLogoUri = if (savedLogoPath != null && File(savedLogoPath).exists()) {
            savedLogoPath
        } else {
            null
        }

        return CompanyProfile(
            companyName = prefs.getString(KEY_NAME, "HAJJI POS - متجر حجّي") ?: "HAJJI POS - متجر حجّي",
            phone = prefs.getString(KEY_PHONE, "01-889900") ?: "01-889900",
            taxNumber = prefs.getString(KEY_TAX_NUMBER, "345678-601") ?: "345678-601",
            address = prefs.getString(KEY_ADDRESS, "بيروت - شارع الحمرا - لبنان") ?: "بيروت - شارع الحمرا - لبنان",
            logoUri = validLogoUri,
            logoPreset = prefs.getString(KEY_LOGO_PRESET, "DEFAULT") ?: "DEFAULT",
            receiptFooterNote = prefs.getString(
                KEY_FOOTER_NOTE,
                "شكراً لزيارتكم! البضاعة المباعة تستبدل خلال 3 أيام مع الفاتورة"
            ) ?: "شكراً لزيارتكم! البضاعة المباعة تستبدل خلال 3 أيام مع الفاتورة"
        )
    }

    /**
     * حفظ التعديلات على الملف التعريفي للشركة
     */
    fun saveCompanyProfile(profile: CompanyProfile) {
        prefs.edit().apply {
            putString(KEY_NAME, profile.companyName)
            putString(KEY_PHONE, profile.phone)
            putString(KEY_TAX_NUMBER, profile.taxNumber)
            putString(KEY_ADDRESS, profile.address)
            putString(KEY_LOGO_URI, profile.logoUri)
            putString(KEY_LOGO_PRESET, profile.logoPreset)
            putString(KEY_FOOTER_NOTE, profile.receiptFooterNote)
            apply()
        }
    }

    /**
     * استيراد شعار من معرض الصور وحفظه كملف دائم في مجلد التطبيق الداخلي
     */
    suspend fun saveCustomLogoFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val destinationFile = File(context.filesDir, LOGO_FILE_NAME)
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { input ->
                val bitmap = BitmapFactory.decodeStream(input) ?: return@withContext null
                FileOutputStream(destinationFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                }
            }
            if (destinationFile.exists() && destinationFile.length() > 0) {
                destinationFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * حذف الشعار المخصص والعودة للشعار الافتراضي
     */
    fun removeCustomLogo() {
        try {
            val logoFile = File(context.filesDir, LOGO_FILE_NAME)
            if (logoFile.exists()) {
                logoFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        prefs.edit().remove(KEY_LOGO_URI).apply()
    }
}
