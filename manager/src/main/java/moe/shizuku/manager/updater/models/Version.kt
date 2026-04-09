package moe.shizuku.manager.updater.models

data class Version(
    private val major: Int,
    private val minor: Int,
    private val patch: Int,
    private val commit: Int = 0,
) : Comparable<Version> {
    override fun compareTo(other: Version): Int =
        compareValuesBy(
            this,
            other,
            { it.major },
            { it.minor },
            { it.patch },
            { it.commit },
        )

    override fun toString(): String =
        if (commit == 0) "$major.$minor.$patch" else "$major.$minor.$patch.r$commit"

    companion object {
        fun parse(tag: String): Version? {
            val regex = Regex("""v?(\d+)\.(\d+)\.(\d+)(?:\.r(\d+))?""")
            val match = regex.find(tag) ?: return null
            val (major, minor, patch, commit) = match.destructured
            return Version(
                major.toInt(),
                minor.toInt(),
                patch.toInt(),
                commit.toIntOrNull() ?: 0
            )
        }
    }
}
