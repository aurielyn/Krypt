package xyz.meowing.krypt.managers.config

data class CategoryElement(val name: String) {
    val features: MutableList<FeatureElement> = mutableListOf()
}