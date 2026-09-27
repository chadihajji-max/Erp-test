package com.example.data.model

/**
 * بيانات الملف التعريفي للشركة (Company / Store Profile)
 * يتضمن اسم الشركة، رقم الهاتف، الرقم الضريبي، العنوان، وشعار الفواتير المخصص.
 */
data class CompanyProfile(
    val companyName: String = "HAJJI POS - متجر حجّي",
    val phone: String = "01-889900",
    val taxNumber: String = "345678-601",
    val address: String = "بيروت - شارع الحمرا - لبنان",
    val logoUri: String? = null, // مسار صورة الشعار المخصص المحفوظ داخلياً
    val logoPreset: String = "DEFAULT", // "DEFAULT", "MINIMAL_POS", "STAMP", "LUXURY"
    val receiptFooterNote: String = "شكراً لزيارتكم! البضاعة المباعة تستبدل خلال 3 أيام مع الفاتورة"
) {
    val hasCustomLogo: Boolean
        get() = !logoUri.isNullOrBlank()
}
