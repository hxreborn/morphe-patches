/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.terabox;

import java.util.List;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

@SuppressWarnings("unused")
public final class AiGeneratedDownload {

    private static final String BRIDGE_NAME = "hxAiGeneratedDownload";

    private static final String AGENT_PATH = "/wap/ai/agent";

    private static final String PREVIEW_PATH = "/wap/ai/filepreview";

    private static final String SOURCE_PATH = "/tera-server-manager/ai/agent/generated/src/";

    private static final String SCRIPT = """
            (() => {
              if (window.__hxAiDownloadInstalled) return;
              window.__hxAiDownloadInstalled = true;
              let selectedCard = null;
              const preview = location.pathname.startsWith('/wap/ai/filepreview');
              const pendingKey = 'hxAiGeneratedDownloadPending';
              const detailPollAttempts = 60;
              const detailPollIntervalMs = 250;
              const pendingMaxAgeMs = 15000;
              async function downloadPreview() {
                try {
                  const docId = new URL(location.href).searchParams.get('doc_id');
                  if (!docId) throw Error('Missing document ID');
                  let request;
                  for (let attempt = 0; attempt < detailPollAttempts; attempt++) {
                    request = performance.getEntriesByType('resource').find(entry => {
                      if (!entry.name.includes('/ai/proxy/file/detail')) return false;
                      return new URL(entry.name).searchParams.get('doc_id') === docId;
                    });
                    if (request) break;
                    await new Promise(resolve => setTimeout(resolve, detailPollIntervalMs));
                  }
                  if (!request) throw Error('Missing file detail');
                  const response = await fetch(request.name, {credentials: 'include'});
                  const result = await response.json();
                  const item = result.data && result.data.item;
                  const pages = item && item.detail && item.detail.page_detail;
                  const source = pages && pages.length === 1 && pages[0].data;
                  if (result.errno !== 0 || !source) throw Error('Missing source file');
                  const sourceUrl = new URL(source);
                  if (sourceUrl.host !== 'storage.googleapis.com' ||
                      !sourceUrl.pathname.startsWith('/tera-server-manager/ai/agent/generated/src/')) {
                    throw Error('Unexpected source file');
                  }
                  const extension = String(item.file_type || sourceUrl.pathname.split('.').pop()).replace(/^\\./, '');
                  const baseName = String(item.title || 'Generated file');
                  const filename = baseName.toLowerCase().endsWith('.' + extension.toLowerCase())
                    ? baseName : baseName + '.' + extension;
                  window.hxAiGeneratedDownload.enqueue(source, filename);
                } catch (error) {
                  window.hxAiGeneratedDownload.failed();
                }
              }
              document.addEventListener('click', event => {
                if (!(event.target instanceof Element)) return;
                const more = event.target.closest('.file-more');
                if (more) {
                  selectedCard = more.closest('.file-card');
                  return;
                }
                const option = event.target.closest('.menu-item');
                if (!option || !option.querySelector('img[src*="download-"]')) return;
                if (!preview && !selectedCard) return;
                event.preventDefault();
                event.stopImmediatePropagation();
                if (preview) {
                  downloadPreview();
                } else {
                  localStorage.setItem(pendingKey, String(Date.now()));
                  selectedCard.querySelector('.file-info').click();
                }
              }, true);
              if (preview) {
                const pending = Number(localStorage.getItem(pendingKey) || 0);
                localStorage.removeItem(pendingKey);
                if (pending && Date.now() - pending < pendingMaxAgeMs) downloadPreview();
              }
            })();
            """;

    private final WebView webView;

    private AiGeneratedDownload(WebView webView) {
        this.webView = webView;
    }

    @SuppressLint("AddJavascriptInterface")
    public static void attach(WebView webView) {
        webView.addJavascriptInterface(new AiGeneratedDownload(webView), BRIDGE_NAME);
    }

    public static void onPageFinished(WebView webView, String url) {
        if (url == null) {
            return;
        }
        Uri page = Uri.parse(url);
        if (!"https".equals(page.getScheme()) || !"www.terabox.com".equals(page.getHost())) {
            return;
        }
        String path = page.getPath();
        if (path != null && (path.startsWith(AGENT_PATH) || path.startsWith(PREVIEW_PATH))) {
            webView.evaluateJavascript(SCRIPT, null);
        }
    }

    @JavascriptInterface
    public void enqueue(String source, String filename) {
        this.webView.post(() -> {
            Uri page = Uri.parse(this.webView.getUrl());
            if (!"www.terabox.com".equals(page.getHost()) || !PREVIEW_PATH.equals(page.getPath())) {
                return;
            }
            Uri uri = Uri.parse(source);
            List<String> path = uri.getPathSegments();
            if (!"https".equals(uri.getScheme()) || !"storage.googleapis.com".equals(uri.getHost())
                    || uri.getPath() == null || !uri.getPath().startsWith(SOURCE_PATH)
                    || uri.getQueryParameter("X-Goog-Signature") == null || path.isEmpty()) {
                showToast("Download failed");
                return;
            }
            String name = filename.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1f]", "_").trim();
            if (name.isEmpty() || name.equals(".") || name.equals("..")) {
                showToast("Download failed");
                return;
            }
            try {
                DownloadManager.Request request = new DownloadManager.Request(uri).setTitle(name)
                    .setMimeType("application/octet-stream")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
                DownloadManager manager = (DownloadManager) this.webView.getContext()
                    .getSystemService(Context.DOWNLOAD_SERVICE);
                manager.enqueue(request);
                showToast("Downloading " + name);
            } catch (RuntimeException exception) {
                showToast("Download failed");
            }
        });
    }

    @JavascriptInterface
    public void failed() {
        this.webView.post(() -> showToast("Download failed"));
    }

    private void showToast(String message) {
        Toast.makeText(this.webView.getContext(), message, Toast.LENGTH_SHORT).show();
    }

}
