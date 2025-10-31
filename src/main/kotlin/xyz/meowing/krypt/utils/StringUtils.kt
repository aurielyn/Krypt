package xyz.meowing.krypt.utils

import xyz.meowing.knit.api.text.core.FormattingCodes.strip

object StringUtils {
    fun String.removeFormatting() = strip(this)
}