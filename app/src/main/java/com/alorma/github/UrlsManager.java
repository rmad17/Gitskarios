package com.alorma.github;

import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.alorma.github.sdk.bean.dto.response.User;
import com.alorma.github.sdk.bean.info.CommitInfo;
import com.alorma.github.sdk.bean.info.FileInfo;
import com.alorma.github.sdk.bean.info.IssueInfo;
import com.alorma.github.sdk.bean.info.RepoInfo;
import com.alorma.github.ui.ErrorHandler;
import com.alorma.github.ui.activity.CommitDetailActivity;
import com.alorma.github.ui.activity.FileActivity;
import com.alorma.github.ui.activity.IssueDetailActivity;
import com.alorma.github.ui.activity.ProfileActivity;
import com.alorma.github.ui.activity.PullRequestDetailActivity;
import com.alorma.github.ui.activity.RepoDetailActivity;
import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;

/**
 * Created by Bernat on 27/04/2015.
 */
public class UrlsManager {

    private static final int URI_BASE = 0;
    private static final int URI_USER = 1;
    private static final int URI_REPO = 2;
    private static final int URI_ISSUE = 3;
    private static final int URI_COMMIT = 4;
    private static final int URI_REPO_BRANCH = 5;
    private static final int URI_REPO_BRANCH_FEATURE = 6;
    private static final int URI_REPO_BRANCH_RELEASE = 7;
    private static final int URI_REPO_BRANCH_HOTFIX = 8;
    private static final int URI_RELEASES_TAG = 9;
    private static final int URI_RELEASES = 10;
    private static final int URI_RELEASES_LATEST = 11;
    private static final int URI_TAGS = 12;
    private static final int URI_PULL_REQUEST = 13;
    private static final int URI_ISSUE_COMMENT = 14;


    private final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private Context context;

    public UrlsManager(Context context) {
        this.context = context;
        int i = 1000;

        uriMatcher.addURI("github.com", "", URI_BASE + i);
        for (String key : context.getResources().getStringArray(R.array.reservedKeys)) {
            uriMatcher.addURI("github.com", key, UriMatcher.NO_MATCH + i++);
        }

        uriMatcher.addURI("github.com", "*/*/releases/latest", URI_RELEASES_LATEST);
        uriMatcher.addURI("github.com", "*/*/releases/tag/*", URI_RELEASES_TAG);
        uriMatcher.addURI("github.com", "*/*/releases", URI_RELEASES);

        uriMatcher.addURI("github.com", "*/*/tags", URI_TAGS);

        uriMatcher.addURI("github.com", "*/*/commit/*", URI_COMMIT);

        uriMatcher.addURI("github.com", "*/*/issues/comments/#", URI_ISSUE_COMMENT);
        uriMatcher.addURI("github.com", "*/*/issues/#", URI_ISSUE);
        uriMatcher.addURI("github.com", "*/*/pull/#", URI_PULL_REQUEST);

        uriMatcher.addURI("github.com", "*/*/tree/feature/*", URI_REPO_BRANCH_FEATURE);
        uriMatcher.addURI("github.com", "*/*/tree/release/*", URI_REPO_BRANCH_RELEASE);
        uriMatcher.addURI("github.com", "*/*/tree/hotfix/*", URI_REPO_BRANCH_HOTFIX);
        uriMatcher.addURI("github.com", "*/*/tree/*", URI_REPO_BRANCH);

        uriMatcher.addURI("github.com", "*/*", URI_REPO);

        uriMatcher.addURI("github.com", "*", URI_USER);
    }

    public void manageUrls(final WebView webView) {
        webView.getSettings().setUseWideViewPort(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Intent intent = checkUri(Uri.parse(url));
                if (intent != null) {
                    view.getContext().startActivity(intent);
                } else {
                    Intent intentView = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    view.getContext().startActivity(intentView);
                }
                return true;
            }
        });
    }

    public Intent checkUri(Uri uri) {

        uri = normalizeUri(uri);

        boolean matched = uriMatcher.match(uri) > -1 && uriMatcher.match(uri) < 1000;
        Intent intent = null;
        if (matched) {
            switch (uriMatcher.match(uri)) {
                case URI_REPO:
                case URI_REPO_BRANCH:
                case URI_REPO_BRANCH_FEATURE:
                case URI_REPO_BRANCH_RELEASE:
                case URI_REPO_BRANCH_HOTFIX:
                    intent = manageRepos(uri);
                    break;
                case URI_USER:
                    intent = manageUsers(uri);
                    break;
                case URI_COMMIT:
                    intent = manageCommit(uri);
                    break;
                case URI_ISSUE:
                    intent = manageIssue(uri);
                    break;
                case URI_PULL_REQUEST:
                    intent = manageIssuePullRequest(uri);
                    break;
                case URI_RELEASES_TAG:
                case URI_TAGS:
                case URI_RELEASES:
                case URI_RELEASES_LATEST:
                case URI_ISSUE_COMMENT:
                    if (Fabric.isInitialized()) {
                        Crashlytics.log(uri.toString());
                    } else {
                        ErrorHandler.onError(context, "Interceptor", new UriNotHandledException(uri));
                    }
                    break;
            }
        } else if (uri.toString().contains("blob")) {
            intent = manageFile(uri);
        } else {
            if (Fabric.isInitialized()) {
                Crashlytics.log(uri.toString());
            }
        }

        return intent;
    }

    private Uri normalizeUri(Uri uri) {
        if (uri.getAuthority().contains("api.")) {
            String authority = uri.getAuthority().replace("api.", "");
            uri = uri.buildUpon().authority(authority).build();
            if (uri.getPath().contains("repos/")) {
                String path = uri.getPath().replace("repos/", "");
                uri = uri.buildUpon().path(path).build();
            }
            if (uri.getPath().contains("commits/")) {
                String path = uri.getPath().replace("commits/", "commit/");
                uri = uri.buildUpon().path(path).build();
            }
            if (uri.getPath().contains("pulls/")) {
                String path = uri.getPath().replace("pulls/", "pull/");
                uri = uri.buildUpon().path(path).build();
            }
        }
        return uri;
    }

    public Intent manageRepos(Uri uri) {

        uri = normalizeUri(uri);

        switch (uriMatcher.match(uri)) {
            case URI_REPO:
            case URI_REPO_BRANCH:
            case URI_REPO_BRANCH_FEATURE:
            case URI_REPO_BRANCH_RELEASE:
            case URI_REPO_BRANCH_HOTFIX:
                RepoInfo repoInfo = extractRepo(uri);
                return RepoDetailActivity.createLauncherIntent(context, repoInfo);
        }
        if (Fabric.isInitialized()) {
            Crashlytics.log(uri.toString());
        }
        return null;
    }

    private RepoInfo extractRepo(Uri uri) {
        RepoInfo repoInfo = new RepoInfo();

        repoInfo.owner = uri.getPathSegments().get(0);
        repoInfo.name = uri.getPathSegments().get(1);

        if (uriMatcher.match(uri) == URI_REPO_BRANCH) {
            repoInfo.branch = uri.getLastPathSegment();
        } else if (uriMatcher.match(uri) == URI_REPO_BRANCH_FEATURE || uriMatcher.match(uri) == URI_REPO_BRANCH_RELEASE || uriMatcher.match(uri) == URI_REPO_BRANCH_HOTFIX) {
            repoInfo.branch = uri.getPathSegments().get(3) + "/" + uri.getPathSegments().get(4);
        }

        return repoInfo;
    }

    public Intent manageUsers(Uri uri) {
        User user = new User();
        user.login = uri.getLastPathSegment();
        return ProfileActivity.createLauncherIntent(context, user);
    }

    private Intent manageCommit(Uri uri) {
        CommitInfo info = new CommitInfo();

        info.repoInfo = extractRepo(uri);

        info.sha = uri.getLastPathSegment();

        return CommitDetailActivity.launchIntent(context, info);
    }

    private Intent manageIssue(Uri uri) {
        IssueInfo info = new IssueInfo();

        info.repoInfo = extractRepo(uri);

        String lastPathSegment = uri.getLastPathSegment();

        if (uri.getFragment() != null && uri.getFragment().contains("issuecomment-")) {
            String commentNum = uri.getFragment().replace("issuecomment-", "");
            info.commentNum = Integer.parseInt(commentNum);
        }
        info.num = Integer.parseInt(lastPathSegment);

        return IssueDetailActivity.createLauncherIntent(context, info);
    }

    private Intent manageIssuePullRequest(Uri uri) {
        IssueInfo info = new IssueInfo();

        info.repoInfo = extractRepo(uri);

        String lastPathSegment = uri.getLastPathSegment();

        info.num = Integer.parseInt(lastPathSegment);

        return PullRequestDetailActivity.createLauncherIntent(context, info);
    }

    private Intent manageFile(Uri uri) {

        FileInfo info = new FileInfo();

        info.repoInfo = extractRepo(uri);
        info.path = uri.getPath();
        if (info.path.contains(info.repoInfo.toString())) {
            info.path = info.path.replace("/" + info.repoInfo.toString() + "/blob/", "");
        }
        info.name = uri.getLastPathSegment();

        return FileActivity.createLauncherIntent(context, info, true);
    }
}
