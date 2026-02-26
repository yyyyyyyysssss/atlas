package com.atlas.common.core.thread;

import com.atlas.common.core.context.UserContext;
import org.springframework.core.task.TaskDecorator;

public class UserContextTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        UserContext.UserObject user = UserContext.getUser();

        return () -> {
            try {
                if (user != null) {
                    UserContext.setUser(user.getUserId(), user.getFullName(),user.isMasking());
                }
                runnable.run();
            } finally {
                UserContext.clear();
            }

        };
    }

}
