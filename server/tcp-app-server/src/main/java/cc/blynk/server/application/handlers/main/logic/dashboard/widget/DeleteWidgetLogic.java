package cc.blynk.server.application.handlers.main.logic.dashboard.widget;

import cc.blynk.server.core.model.DashBoard;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.server.core.model.widgets.ui.Tabs;
import cc.blynk.server.core.protocol.exceptions.IllegalCommandException;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.utils.ArrayUtil;
import cc.blynk.utils.ParseUtil;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static cc.blynk.utils.ByteBufUtil.ok;
import static cc.blynk.utils.StringUtils.split2;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 01.02.16.
 */
public class DeleteWidgetLogic {

    private static final Logger log = LogManager.getLogger(DeleteWidgetLogic.class);

    public static void messageReceived(ChannelHandlerContext ctx, User user, StringMessage message) {
        String[] split = split2(message.body);

        if (split.length < 2) {
            throw new IllegalCommandException("Wrong income message format.");
        }

        int dashId = ParseUtil.parseInt(split[0]) ;
        long widgetId = ParseUtil.parseLong(split[1]);

        DashBoard dash = user.profile.getDashByIdOrThrow(dashId);

        log.debug("Removing widget with id {}.", widgetId);

        int existingWidgetIndex = dash.getWidgetIndexById(widgetId);
        Widget widgetToDelete = dash.widgets[existingWidgetIndex];
        if (widgetToDelete instanceof Tabs) {
            deleteTabs(user, dash, 0);
        }

        existingWidgetIndex = dash.getWidgetIndexById(widgetId);
        deleteWidget(user, dash, existingWidgetIndex);

        ctx.writeAndFlush(ok(message.id), ctx.voidPromise());
    }

    /**
     * Removes all widgets with tabId greater than lastTabIndex
     */
    public static void deleteTabs(User user, DashBoard dash, int lastTabIndex) {
        List<Widget> zeroTabWidgets = new ArrayList<>();
        int removedWidgetPrice = 0;
        for (Widget widget : dash.widgets) {
            if (widget.tabId > lastTabIndex) {
                removedWidgetPrice += widget.getPrice();
            } else {
                zeroTabWidgets.add(widget);
            }
        }

        user.recycleEnergy(removedWidgetPrice);
        dash.widgets = zeroTabWidgets.toArray(new Widget[zeroTabWidgets.size()]);
        dash.updatedAt = System.currentTimeMillis();
    }

    private static void deleteWidget(User user, DashBoard dash, int existingWidgetIndex) {
        user.recycleEnergy(dash.widgets[existingWidgetIndex].getPrice());
        dash.widgets = ArrayUtil.remove(dash.widgets, existingWidgetIndex);
        dash.updatedAt = System.currentTimeMillis();
    }

}
