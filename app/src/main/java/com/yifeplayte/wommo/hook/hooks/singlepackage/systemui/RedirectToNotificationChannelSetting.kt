package com.yifeplayte.wommo.hook.hooks.singlepackage.systemui

import android.content.Intent
import android.os.UserHandle
import android.provider.Settings
import android.service.notification.StatusBarNotification
import android.widget.ImageView
import com.github.kyuubiran.ezxhelper.ClassUtils.getStaticObjectOrNullAs
import com.github.kyuubiran.ezxhelper.ClassUtils.invokeStaticMethodBestMatch
import com.github.kyuubiran.ezxhelper.ClassUtils.loadClass
import com.github.kyuubiran.ezxhelper.EzXHelper.appContext
import com.github.kyuubiran.ezxhelper.EzXHelper.initAppContext
import com.github.kyuubiran.ezxhelper.HookFactory.`-Static`.createHook
import com.github.kyuubiran.ezxhelper.ObjectHelper.Companion.objectHelper
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNull
import com.github.kyuubiran.ezxhelper.ObjectUtils.getObjectOrNullAs
import com.github.kyuubiran.ezxhelper.ObjectUtils.invokeMethodBestMatch
import com.github.kyuubiran.ezxhelper.finders.MethodFinder.`-Static`.methodFinder
import com.yifeplayte.wommo.hook.hooks.BaseHook
import com.yifeplayte.wommo.utils.Build.IS_HYPER_OS

@Suppress("unused")
object RedirectToNotificationChannelSetting : BaseHook() {
    override val key: String = "redirect_to_notification_channel_setting"
    override fun hook() {
        var statusBarNotification: StatusBarNotification? = null
        val clazzMiuiNotificationMenuRow =
            loadClass("com.android.systemui.statusbar.notification.row.MiuiNotificationMenuRow")
        if (IS_HYPER_OS) {
            val clazzDependency = loadClass("com.android.systemui.Dependency")
            val clazzModalController =
                loadClass("com.android.systemui.statusbar.notification.modal.ModalController")
            val clazzCommandQueue = loadClass("com.android.systemui.statusbar.CommandQueue")
            clazzMiuiNotificationMenuRow.methodFinder().filterByName("createMenuViews").first()
                .createHook {
                    after { param ->
                        val mSbn =
                            getObjectOrNullAs<StatusBarNotification>(param.thisObject, "mSbn")
                                ?: return@after
                        val mInfoItem =
                            getObjectOrNull(param.thisObject, "mInfoItem") ?: return@after
                        initAppContext(getObjectOrNullAs(param.thisObject, "mContext"))
                        val mIcon = getObjectOrNullAs<ImageView>(mInfoItem, "mIcon") ?: return@after
                        mIcon.setOnClickListener {
                            startChannelNotificationSettings(mSbn)
                            val modalController = invokeStaticMethodBestMatch(
                                clazzDependency, "get", null, clazzModalController
                            ) ?: return@setOnClickListener
                            invokeMethodBestMatch(
                                modalController, "animExitModal", null, 50L, true, "MORE", false
                            )
                            val commandQueue = invokeStaticMethodBestMatch(
                                clazzDependency, "get", null, clazzCommandQueue
                            ) ?: return@setOnClickListener
                            invokeMethodBestMatch(
                                commandQueue, "animateCollapsePanels", null, 0, false
                            )
                        }
                    }
                }
        }
        clazzMiuiNotificationMenuRow.methodFinder().filterByName("onClickInfoItem").firstOrNull()
            ?.createHook {
                before { param ->
                    param.thisObject.objectHelper {
                        initAppContext(getObjectOrNullAs("mContext"))
                        statusBarNotification = getObjectOrNullAs("mSbn")
                    }
                }
                after {
                    statusBarNotification = null
                }
            }
        loadClass("com.android.systemui.statusbar.notification.NotificationSettingsHelper").methodFinder()
            .filterByName("startAppNotificationSettings").firstOrNull()?.createHook {
                before { param ->
                    startChannelNotificationSettings(statusBarNotification!!)
                    param.result = null
                }
            }
    }

    private fun startChannelNotificationSettings(statusBarNotification: StatusBarNotification) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setClassName(
                "com.android.settings", "com.android.settings.SubSettings"
            )
            putExtra(
                ":android:show_fragment",
                "com.android.settings.notification.ChannelNotificationSettings"
            )
            putExtra(
                Settings.EXTRA_APP_PACKAGE, statusBarNotification.packageName
            )
            putExtra(
                Settings.EXTRA_CHANNEL_ID, statusBarNotification.notification.channelId
            )
            putExtra("app_uid", statusBarNotification.uid).putExtra(
                Settings.EXTRA_CONVERSATION_ID, statusBarNotification.notification.shortcutId
            )
        }
        val userHandleCurrent = getStaticObjectOrNullAs<UserHandle>(
            UserHandle::class.java, "CURRENT"
        )
        invokeMethodBestMatch(
            appContext, "startActivityAsUser", null, intent, userHandleCurrent
        )
    }
}