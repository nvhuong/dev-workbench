package com.nvhuong.devworkbench.panel.workspace.components;

import com.nvhuong.devworkbench.common.constants.ModernColors;
import com.nvhuong.devworkbench.model.GitOperation;
import lombok.experimental.UtilityClass;

import java.awt.Color;

@UtilityClass
public class GitOperationPresentation {

    public String getIconName(GitOperation operation) {
        return switch (operation) {
            case COMMIT -> "icons/git-commit.svg";
            case PUSH -> "icons/git-push.svg";
            case PULL -> "icons/git-pull.svg";
        };
    }

    public Color getColor(GitOperation operation) {
        return switch (operation) {
            case COMMIT -> ModernColors.getGitCommit();
            case PUSH -> ModernColors.getGitPush();
            case PULL -> ModernColors.getGitPull();
        };
    }
}
