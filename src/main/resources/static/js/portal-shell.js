/* CampusCare commercial academic portal shell.
 * Uses the existing authenticated APIs; does not mock backend data.
 * Load csrf.js before this file so mutations include X-XSRF-TOKEN.
 */
(function () {
    "use strict";

    const path = window.location.pathname.toLowerCase();
    const isLoginPage = path.endsWith("/login.html") || Boolean(document.getElementById("loginForm"));
    const rolePages = {
        STUDENT: "/student.html",
        STAFF: "/staff.html",
        WARDEN: "/warden.html",
        ADMIN: "/admin.html"
    };

    function svg(name, size = 20) {
        const common = `width="${size}" height="${size}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"`;
        const paths = {
            building: '<path d="M3 21h18M5 21V7l7-4 7 4v14M9 21v-5h6v5M9 9h.01M15 9h.01M9 12h.01M15 12h.01"/>',
            bell: '<path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9"/><path d="M10 21h4"/>',
            chevron: '<path d="m6 9 6 6 6-6"/>',
            user: '<circle cx="12" cy="8" r="4"/><path d="M4 21a8 8 0 0 1 16 0"/>',
            logout: '<path d="M10 17l5-5-5-5M15 12H3"/><path d="M12 3h6a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-6"/>',
            check: '<path d="m5 12 4 4L19 6"/>',
            external: '<path d="M14 3h7v7M10 14 21 3"/><path d="M21 14v5a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5"/>'
        };
        return `<svg ${common}>${paths[name] || paths.building}</svg>`;
    }

    function element(tag, className, text) {
        const node = document.createElement(tag);
        if (className) node.className = className;
        if (text !== undefined && text !== null) node.textContent = String(text);
        return node;
    }

    function initials(name) {
        const parts = String(name || "User").trim().split(/\s+/).filter(Boolean);
        return (parts.length > 1 ? parts[0][0] + parts[parts.length - 1][0] : (parts[0] || "U").slice(0, 2)).toUpperCase();
    }

    function formatDate(value) {
        if (!value) return "Date unavailable";
        const date = new Date(value);
        return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString();
    }

    async function requestJson(url, options = {}) {
        const response = await fetch(url, {
            credentials: "same-origin",
            cache: "no-store",
            ...options
        });
        const text = await response.text();
        let data = null;
        if (text) {
            try { data = JSON.parse(text); }
            catch { data = { message: text }; }
        }
        if (!response.ok) {
            const message = data && (data.message || data.detail || data.error);
            throw new Error(message || `Request failed (HTTP ${response.status})`);
        }
        return data;
    }

    function roleName(role) {
        const names = {
            STUDENT: "Student portal",
            STAFF: "Maintenance staff",
            WARDEN: "Warden administration",
            ADMIN: "System administrator"
        };
        return names[role] || "Hostel services";
    }

    function dashboardLinks(role) {
        const links = {
            STUDENT: [
                ["Submit complaint", "#complaintForm"],
                ["My complaints", "#complaintList"]
            ],
            STAFF: [
                ["Work queue", "#assignmentList"],
                ["Assignment overview", "#viewTitle"]
            ],
            WARDEN: [
                ["SLA escalations", "#escalationTable"],
                ["Escalation overview", "#totalCount"]
            ],
            ADMIN: [
                ["Create warden", "#wardenForm"],
                ["Staff accounts", "#staffForm"]
            ]
        };
        return links[role] || [];
    }

    function makeIconButton(title, iconName, className) {
        const button = element("button", className, "");
        button.type = "button";
        button.setAttribute("aria-label", title);
        button.title = title;
        button.innerHTML = svg(iconName, 20);
        return button;
    }

    function makeShell(user) {
        document.body.classList.add("portal-shell-active");
        const existingHeader = document.querySelector("body > header, body > main > header, body header");
        if (existingHeader) existingHeader.classList.add("portal-page-intro");

        const shell = element("div", "portal-topbar");
        shell.innerHTML = `
            <div class="portal-topbar-inner">
                <a class="portal-brand" href="${rolePages[user.role] || "/index.html"}" aria-label="CampusCare home">
                    <span class="portal-brand-mark">${svg("building", 23)}</span>
                    <span class="portal-brand-copy"><strong>CampusCare</strong><small>HOSTEL SERVICES PORTAL</small></span>
                </a>
                <nav class="portal-primary-nav" aria-label="Portal navigation"></nav>
                <div class="portal-actions">
                    <div class="portal-action-wrap portal-bell-wrap">
                        <button type="button" class="portal-icon-button portal-bell-button" aria-label="View notifications" aria-expanded="false" aria-controls="portalNotificationPopover">
                            ${svg("bell", 21)}<span class="portal-unread-badge" hidden></span>
                        </button>
                        <section class="portal-popover portal-notifications-popover" id="portalNotificationPopover" aria-label="Notifications" hidden>
                            <div class="portal-popover-heading"><div><strong class="portal-popover-title">Notifications</strong><span class="portal-popover-subtitle">Your latest updates</span></div><button type="button" class="portal-text-button portal-notifications-refresh">Refresh</button></div>
                            <div class="portal-notification-list"><div class="portal-notification-state">Loading notifications…</div></div>
                        </section>
                    </div>
                    <div class="portal-action-wrap portal-profile-wrap">
                        <button type="button" class="portal-profile-button" aria-expanded="false" aria-controls="portalProfilePopover">
                            <span class="portal-avatar">${initials(user.fullName)}</span>
                            <span class="portal-profile-copy"><strong class="portal-profile-name"></strong><small class="portal-profile-role"></small></span>
                            ${svg("chevron", 16)}
                        </button>
                        <section class="portal-popover portal-profile-popover" id="portalProfilePopover" aria-label="Profile menu" hidden>
                            <div class="portal-profile-menu-head"><span class="portal-avatar portal-avatar-large">${initials(user.fullName)}</span><div><strong class="portal-profile-menu-name"></strong><span class="portal-role-pill"></span></div></div>
                            <div class="portal-profile-menu-meta"><span>Username</span><strong class="portal-menu-username"></strong></div>
                            <div class="portal-profile-menu-meta portal-menu-staff-row" hidden><span>Staff profile ID</span><strong class="portal-menu-staff-id"></strong></div>
                            <button type="button" class="portal-menu-action portal-view-profile">${svg("user", 17)}View profile details</button>
                            <button type="button" class="portal-menu-action portal-logout-button">${svg("logout", 17)}Sign out</button>
                            <p class="portal-menu-message" role="status" hidden></p>
                        </section>
                    </div>
                </div>
            </div>`;

        const nav = shell.querySelector(".portal-primary-nav");
        dashboardLinks(user.role).forEach(([label, href]) => {
            const anchor = element("a", "portal-nav-link", label);
            anchor.href = href;
            anchor.addEventListener("click", event => {
                const target = href.startsWith("#") ? document.querySelector(href) : null;
                if (target) {
                    event.preventDefault();
                    target.scrollIntoView({ behavior: "smooth", block: "start" });
                }
            });
            nav.appendChild(anchor);
        });

        shell.querySelector(".portal-profile-name").textContent = user.fullName || user.username || "Account";
        shell.querySelector(".portal-profile-role").textContent = roleName(user.role);
        shell.querySelector(".portal-profile-menu-name").textContent = user.fullName || user.username || "Account";
        shell.querySelector(".portal-role-pill").textContent = user.role || "USER";
        shell.querySelector(".portal-menu-username").textContent = user.username || "—";
        if (user.staffId != null) {
            shell.querySelector(".portal-menu-staff-row").hidden = false;
            shell.querySelector(".portal-menu-staff-id").textContent = String(user.staffId);
        }

        // Keep the portal bar at the document body level so it spans the viewport,
        // including pages whose own header is nested inside <main>.
        document.body.insertBefore(shell, document.body.firstChild);

        const profileButton = shell.querySelector(".portal-profile-button");
        const bellButton = shell.querySelector(".portal-bell-button");
        const profilePopover = shell.querySelector(".portal-profile-popover");
        const notificationPopover = shell.querySelector(".portal-notifications-popover");
        const bellBadge = shell.querySelector(".portal-unread-badge");
        const list = shell.querySelector(".portal-notification-list");
        const message = shell.querySelector(".portal-menu-message");

        let notifications = [];
        let studentActivities = [];

        function closeMenus() {
            profilePopover.hidden = true;
            notificationPopover.hidden = true;
            profileButton.setAttribute("aria-expanded", "false");
            bellButton.setAttribute("aria-expanded", "false");
        }

        profileButton.addEventListener("click", event => {
            event.stopPropagation();
            const open = profilePopover.hidden;
            closeMenus();
            profilePopover.hidden = !open;
            profileButton.setAttribute("aria-expanded", String(open));
        });

        bellButton.addEventListener("click", async event => {
            event.stopPropagation();
            const open = notificationPopover.hidden;
            closeMenus();
            notificationPopover.hidden = !open;
            bellButton.setAttribute("aria-expanded", String(open));
            if (open) await refreshNotifications();
        });

        profilePopover.addEventListener("click", event => event.stopPropagation());
        notificationPopover.addEventListener("click", event => event.stopPropagation());
        document.addEventListener("click", closeMenus);
        document.addEventListener("keydown", event => {
            if (event.key === "Escape") closeMenus();
        });

        shell.querySelector(".portal-view-profile").addEventListener("click", () => {
            closeMenus();
            openProfileDialog(user);
        });

        shell.querySelector(".portal-logout-button").addEventListener("click", async () => {
            const button = shell.querySelector(".portal-logout-button");
            button.disabled = true;
            message.hidden = false;
            message.textContent = "Signing out…";
            try {
                await requestJson("/api/auth/logout", { method: "POST" });
                window.location.replace("/login.html");
            } catch (error) {
                message.textContent = "Sign-out failed. Please try again.";
                button.disabled = false;
            }
        });

        shell.querySelector(".portal-notifications-refresh").addEventListener("click", refreshNotifications);

        function renderMessage(text) {
            list.replaceChildren(element("div", "portal-notification-state", text));
        }

        function setUnreadCount(count) {
            const value = Math.max(0, Number(count) || 0);
            if (value > 0) {
                bellBadge.hidden = false;
                bellBadge.textContent = value > 99 ? "99+" : String(value);
                bellButton.setAttribute("aria-label", `View notifications, ${value} unread`);
            } else {
                bellBadge.hidden = true;
                bellBadge.textContent = "";
                bellButton.setAttribute("aria-label", "View notifications");
            }
        }

        function isRead(item) {
            return item && (item.read === true || item.isRead === true);
        }

        function addNotificationCard(item, kind) {
            const card = element("article", "portal-notification-item" + (kind === "student" ? " portal-activity-item" : (isRead(item) ? "" : " is-unread")));
            const heading = element("div", "portal-notification-item-top");
            const title = kind === "student"
                ? `Complaint #${item.complaintId ?? "—"} · ${String(item.status || "STATUS UPDATE").replaceAll("_", " ")}`
                : (item.title || "Notification");
            heading.appendChild(element("strong", "portal-notification-item-title", title));
            if (kind !== "student") {
                heading.appendChild(element("span", "portal-notification-state-badge" + (isRead(item) ? "" : " unread"), isRead(item) ? "Read" : "Unread"));
            } else {
                heading.appendChild(element("span", "portal-status-chip", String(item.status || "UNKNOWN").replaceAll("_", " ")));
            }
            card.appendChild(heading);
            card.appendChild(element("p", "portal-notification-item-message", kind === "student" ? (item.title || "Your complaint has been updated.") : (item.message || "There is an update for your account.")));
            if (kind !== "student") {
                const details = [];
                if (item.complaintId != null) details.push("Complaint #" + item.complaintId);
                if (item.assignmentId != null) details.push("Assignment #" + item.assignmentId);
                if (item.eventType) details.push(String(item.eventType).replaceAll("_", " "));
                if (details.length) card.appendChild(element("div", "portal-notification-item-meta", details.join(" · ")));
            }
            card.appendChild(element("div", "portal-notification-item-date", formatDate(item.createdAt)));

            if (kind !== "student" && !isRead(item)) {
                const button = element("button", "portal-mark-read", "Mark as read");
                button.type = "button";
                button.addEventListener("click", async () => {
                    button.disabled = true;
                    button.textContent = "Saving…";
                    try {
                        const isStaff = user.role === "STAFF";
                        const recipientType = isStaff ? "STAFF" : "WARDEN";
                        const recipientId = isStaff ? `&recipientId=${encodeURIComponent(user.staffId)}` : "";
                        const endpoint = `/api/notifications/${encodeURIComponent(item.notificationId)}/read?recipientType=${recipientType}${recipientId}`;
                        await requestJson(endpoint, { method: "PATCH" });
                        await refreshNotifications();
                    } catch (error) {
                        button.disabled = false;
                        button.textContent = "Retry mark as read";
                        button.title = error.message;
                    }
                });
                card.appendChild(button);
            }
            list.appendChild(card);
        }

        async function refreshNotifications() {
            const refreshButton = shell.querySelector(".portal-notifications-refresh");
            refreshButton.disabled = true;
            refreshButton.textContent = "Loading…";
            try {
                if (user.role === "STUDENT") {
                    const data = await requestJson("/api/complaints");
                    studentActivities = Array.isArray(data) ? data.slice().sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0)).slice(0, 6) : [];
                    shell.querySelector(".portal-popover-title").textContent = "Complaint activity";
                    shell.querySelector(".portal-popover-subtitle").textContent = "Recent status of your complaints";
                    setUnreadCount(0);
                    list.replaceChildren();
                    if (!studentActivities.length) {
                        renderMessage("No complaint activity yet. Submit a complaint to get started.");
                    } else {
                        studentActivities.forEach(item => addNotificationCard(item, "student"));
                        const link = element("a", "portal-notification-footer-link", "View all my complaints");
                        link.href = "#complaintList";
                        link.addEventListener("click", event => {
                            const target = document.querySelector("#complaintList");
                            if (target) { event.preventDefault(); closeMenus(); target.scrollIntoView({ behavior: "smooth" }); }
                        });
                        list.appendChild(link);
                    }
                    return;
                }

                shell.querySelector(".portal-popover-title").textContent = user.role === "ADMIN" ? "Warden notifications" : "Notifications";
                shell.querySelector(".portal-popover-subtitle").textContent = "Saved updates from the system";
                let endpoint;
                if (user.role === "STAFF") {
                    if (user.staffId == null) throw new Error("This account is not linked to a staff profile.");
                    endpoint = `/api/notifications/staff/${encodeURIComponent(user.staffId)}`;
                } else if (user.role === "WARDEN" || user.role === "ADMIN") {
                    endpoint = "/api/notifications/warden";
                } else {
                    throw new Error("Notifications are not available for this role.");
                }
                const data = await requestJson(endpoint);
                notifications = Array.isArray(data) ? data : [];
                const unread = notifications.filter(item => !isRead(item)).length;
                setUnreadCount(unread);
                list.replaceChildren();
                if (!notifications.length) {
                    renderMessage("You're all caught up. No saved notifications yet.");
                } else {
                    notifications.slice(0, 8).forEach(item => addNotificationCard(item, "system"));
                    if (notifications.length > 8) {
                        list.appendChild(element("div", "portal-notification-more", `${notifications.length - 8} older notifications available in the dashboard history.`));
                    }
                }

                // Keep the existing notification DOM and handlers intact, but avoid
                // duplicating the same content inline once the bell feed is available.
                hideNativeNotificationPanel();
            } catch (error) {
                renderMessage("Could not load updates. " + error.message);
            } finally {
                refreshButton.disabled = false;
                refreshButton.textContent = "Refresh";
            }
        }

        // Initial unread count and periodic refresh, without opening the popover.
        refreshNotifications();
        window.setInterval(() => {
            if (!document.hidden) refreshNotifications();
        }, 60000);
    }

    function hideNativeNotificationPanel() {
        const heading = document.getElementById("savedNotificationsHeading")
            || Array.from(document.querySelectorAll("h1,h2,h3,h4")).find(node => /saved notifications/i.test(node.textContent || ""));
        let panel = heading ? heading.closest("section") : null;
        if (!panel && heading) panel = heading.closest(".panel, .persistent-panel, article, div");
        if (panel && !panel.classList.contains("portal-topbar")) {
            panel.classList.add("portal-native-notifications-hidden");
            panel.setAttribute("aria-hidden", "true");
        }
    }

    function openProfileDialog(user) {
        let dialog = document.getElementById("portalProfileDialog");
        if (!dialog) {
            dialog = element("dialog", "portal-profile-dialog");
            dialog.id = "portalProfileDialog";
            const card = element("div", "portal-profile-dialog-card");
            const top = element("div", "portal-profile-dialog-top");
            top.appendChild(element("div", "portal-profile-dialog-eyebrow", "ACCOUNT DETAILS"));
            const close = element("button", "portal-dialog-close", "×");
            close.type = "button";
            close.setAttribute("aria-label", "Close profile details");
            close.addEventListener("click", () => dialog.close());
            top.appendChild(close);
            card.appendChild(top);
            const main = element("div", "portal-profile-dialog-hero");
            main.appendChild(element("span", "portal-avatar portal-avatar-large", initials(user.fullName)));
            const named = element("div", "");
            named.appendChild(element("h2", "portal-profile-dialog-name", user.fullName || user.username || "Account"));
            named.appendChild(element("span", "portal-role-pill", roleName(user.role)));
            main.appendChild(named);
            card.appendChild(main);
            const grid = element("div", "portal-profile-details");
            const addDetail = (label, value) => {
                const item = element("div", "portal-profile-detail");
                item.appendChild(element("span", "", label));
                item.appendChild(element("strong", "", value == null || value === "" ? "Not linked" : value));
                grid.appendChild(item);
            };
            addDetail("Full name", user.fullName);
            addDetail("Username", user.username);
            addDetail("Role", user.role);
            addDetail("Account ID", user.userId);
            if (String(user.role).toUpperCase() === "STAFF") addDetail("Staff profile ID", user.staffId);
            card.appendChild(grid);
            card.appendChild(element("p", "portal-profile-dialog-note", "Profile details are managed by the system administrator."));
            dialog.appendChild(card);
            dialog.addEventListener("click", event => { if (event.target === dialog) dialog.close(); });
            document.body.appendChild(dialog);
        }
        if (typeof dialog.showModal === "function") dialog.showModal();
        else dialog.setAttribute("open", "open");
    }

    function addLoginBrand() {
        document.body.classList.add("portal-login-page");
        if (document.querySelector(".portal-login-brand")) return;
        const brand = element("a", "portal-login-brand");
        brand.href = "/login.html";
        brand.innerHTML = `<span class="portal-brand-mark">${svg("building", 23)}</span><span class="portal-brand-copy"><strong>CampusCare</strong><small>HOSTEL SERVICES PORTAL</small></span>`;
        document.body.insertBefore(brand, document.body.firstChild);
    }

    async function init() {
        if (isLoginPage) {
            addLoginBrand();
            return;
        }
        try {
            const user = await requestJson("/api/auth/me");
            makeShell(user);
        } catch (error) {
            // The endpoint is protected. Do not create a fake profile if a session is gone.
            if (path !== "/index.html" && path !== "/") {
                window.location.replace("/login.html?expired");
            }
        }
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init, { once: true });
    } else {
        init();
    }
})();
