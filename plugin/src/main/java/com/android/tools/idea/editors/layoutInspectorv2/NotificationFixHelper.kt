package com.android.tools.idea.editors.layoutInspectorv2

import com.intellij.notification.*
import org.jetbrains.android.util.AndroidBundle
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import com.intellij.openapi.project.Project

object NotificationFixHelper {
    fun createNotification(message: String, type: NotificationType, project: Project) {
        try {
            val notificationGroupManagerClass = Class.forName("com.intellij.notification.NotificationGroupManager")
            val getInstanceMethod: Method = notificationGroupManagerClass.getDeclaredMethod("getInstance")
            val notificationGroupManager = getInstanceMethod.invoke(null)

            val getNotificationGroupMethod: Method = notificationGroupManager.javaClass.getDeclaredMethod(
                "getNotificationGroup",
                String::class.java
            )
            val notificationGroup = getNotificationGroupMethod.invoke(notificationGroupManager, "Layout Inspector V2 Pro")

            val createNotificationMethod: Method = notificationGroup.javaClass.getDeclaredMethod(
                "createNotification",
                String::class.java,
                NotificationType::class.java
            )
            val notification = createNotificationMethod.invoke(notificationGroup, message, type)

            val notifyMethod: Method = notification.javaClass.getDeclaredMethod(
                "notify",
                Project::class.java
            )
            notifyMethod.invoke(notification, project)
        } catch (e: Exception) {
            // using Notifications.Bus.notify api
            usingNotificationBus(message, type)
        }
    }


    private fun usingNotificationBus(message: String, type: NotificationType) {

        val title = "Layout Inspector"

        try {
            val notificationGroupClass = Class.forName("com.intellij.notification.NotificationGroup")
            val createIdWithTitleMethod: Method = notificationGroupClass.getDeclaredMethod(
                "createIdWithTitle",
                String::class.java,
                String::class.java
            )
            val groupId: String = createIdWithTitleMethod.invoke(null, title, AndroidBundle.message("android.ddms.actions.layoutinspector.notification.group")) as String

            val notificationClass = Class.forName("com.intellij.notification.Notification")
            val constructor: Constructor<*> = notificationClass.getDeclaredConstructor(
                String::class.java,
                String::class.java,
                String::class.java,
                NotificationType::class.java,
                NotificationListener::class.java
            )
            val notification: Notification = constructor.newInstance(groupId, title, message, type, null) as Notification

            // use notification object as needed

            val notificationBusClass = Class.forName("com.intellij.notification.Notifications\$Bus")
            val notifyMethod: Method = notificationBusClass.getDeclaredMethod(
                "notify",
                Notification::class.java
            )
            notifyMethod.invoke(null, notification)
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: InstantiationException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }

    }

}