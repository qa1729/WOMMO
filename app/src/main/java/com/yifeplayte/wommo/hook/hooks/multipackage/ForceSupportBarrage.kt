package com.yifeplayte.wommo.hook.hooks.multipackage

import android.annotation.SuppressLint
import android.app.Notification
import android.content.Context
import android.content.pm.ApplicationInfo
import android.service.notification.StatusBarNotification
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.EzXHelper.safeClassLoader
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNullAs
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseMultiHook
import com.yifeplayte.wommo.hook.utils.DexKit.dexKitBridge

@Suppress("unused")
object ForceSupportBarrage : BaseMultiHook() {
    override val key = "force_support_barrage"
    override val hooks = mapOf(
        "com.xiaomi.barrage" to { hookForBarrage() },
        "com.miui.securitycenter" to { hookForSecurityCenter() },
    )

    @SuppressLint("QueryPermissionsNeeded")
    private fun hookForSecurityCenter() {
        val clazzNotificationFilterHelper = loadClass("miui.util.NotificationFilterHelper")
        val methodAreNotificationsEnabled = clazzNotificationFilterHelper.getDeclaredMethod(
            "areNotificationsEnabled", Context::class.java, String::class.java
        ).apply { isAccessible = true }
        dexKitBridge.findMethod {
            matcher {
                usingStrings = listOf("getInstance().assets.open(_SUPPORT_APPS_FILE_NAME)")
            }
        }.single().getMethodInstance(safeClassLoader).createHook {
            after { param ->
                val barragePackageList = appContext.packageManager.getInstalledApplications(0)
                    .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) != 1 }
                    .map { it.packageName }.filter {
                        methodAreNotificationsEnabled.invoke(
                            clazzNotificationFilterHelper, appContext, it
                        ) == true
                    }
                @Suppress("UNCHECKED_CAST") val supportedList = param.result as MutableList<String>
                for (s in barragePackageList) {
                    if (!supportedList.contains(s)) supportedList.add(s)
                }
            }
        }
    }

    private fun hookForBarrage() {
        loadClass("com.xiaomi.barrage.service.NotificationMonitorService").methodFinder()
            .filterByName("filterNotification").single().createHook {
                before { param ->
                    val statusBarNotification = param.args[0] as StatusBarNotification
                    val packageName = statusBarNotification.packageName
                    getObjectOrNullAs<ArrayList<String>>(
                        param.thisObject, "mBarragePackageList"
                    )!!.apply { if (!contains(packageName)) add(packageName) }
                    if (statusBarNotification.shouldBeFiltered()) {
                        param.result = true
                    }
                }
            }
    }

    object NotificationCache {
        private const val MAX_SIZE = 100
        private val cache = LinkedHashSet<String>()
        fun check(string: String): Boolean {
            val result = cache.add(string)
            if (cache.size > MAX_SIZE) cache.remove(cache.first())
            return result
        }
    }

    private fun StatusBarNotification.shouldBeFiltered(): Boolean {
        val extras = notification.extras
        val key =
            "${extras.getCharSequence("android.title")}: ${extras.getCharSequence("android.text")}"
        val isGroupSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0
        return !isClearable || isGroupSummary || !NotificationCache.check(key)
    }
}