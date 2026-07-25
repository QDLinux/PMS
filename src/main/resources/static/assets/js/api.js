(function () {
    const TOKEN_KEY = "pm_token";
    const USERNAME_KEY = "pm_username";
    const SAVED_USERNAME_KEY = "pm_saved_username";
    const DASHBOARD_MODE_KEY = "pm_dashboard_mode";

    function getFeatureFlags() {
        const defaults = {
            dashboardMode: "vue",
            vueDashboardPath: "/app/index.html",
            legacyDashboardPath: "/dashboard.html",
            forceLegacyQueryKey: "legacyDashboard"
        };
        return Object.assign(defaults, window.PMFeatureFlags || {});
    }

    function getDashboardMode() {
        const saved = (localStorage.getItem(DASHBOARD_MODE_KEY) || "").trim().toLowerCase();
        if (saved === "vue" || saved === "legacy") {
            return saved;
        }
        const flags = getFeatureFlags();
        return String(flags.dashboardMode || "legacy").toLowerCase() === "vue" ? "vue" : "legacy";
    }

    function setDashboardMode(mode) {
        const normalized = mode === "vue" ? "vue" : "legacy";
        localStorage.setItem(DASHBOARD_MODE_KEY, normalized);
    }

    function getDashboardEntryUrl() {
        const flags = getFeatureFlags();
        return getDashboardMode() === "vue"
            ? (flags.vueDashboardPath || "/app/index.html")
            : (flags.legacyDashboardPath || "/dashboard.html");
    }

    function getToken() {
        return localStorage.getItem(TOKEN_KEY) || "";
    }

    function setAuth(token, username, remember, expiresInSeconds) {
        clearAuth();
        localStorage.setItem(TOKEN_KEY, token);
        localStorage.setItem(USERNAME_KEY, username || "");

        if (remember && username) {
            localStorage.setItem(SAVED_USERNAME_KEY, username);
        } else {
            localStorage.removeItem(SAVED_USERNAME_KEY);
        }
    }

    function clearAuth() {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USERNAME_KEY);
    }

    function getUsername() {
        return localStorage.getItem(USERNAME_KEY) || "";
    }

    function getSavedUsername() {
        return localStorage.getItem(SAVED_USERNAME_KEY) || "";
    }

    async function request(url, options) {
        const opts = options || {};
        const isFormData = opts.body instanceof FormData;
        const headers = Object.assign({}, opts.headers || {});
        if (!isFormData && !headers["Content-Type"]) {
            headers["Content-Type"] = "application/json";
        }
        const token = getToken();
        if (token) {
            headers.Authorization = "Bearer " + token;
        }

        const response = await fetch(url, Object.assign({}, opts, { headers: headers }));
        const isJson = (response.headers.get("content-type") || "").includes("application/json");
        let payload = null;
        if (isJson) {
            try {
                payload = await response.json();
            } catch (e) {
                payload = null;
            }
        }

        if (!response.ok || (payload && payload.success === false)) {
            const msg = (payload && (payload.message || payload.error)) || "请求失败";
            const err = new Error(msg);
            err.status = response.status;
            throw err;
        }

        return payload ? payload.data : null;
    }

    window.AppApi = {
        getToken: getToken,
        setAuth: setAuth,
        clearAuth: clearAuth,
        getUsername: getUsername,
        getSavedUsername: getSavedUsername,
        getDashboardMode: getDashboardMode,
        setDashboardMode: setDashboardMode,
        getDashboardEntryUrl: getDashboardEntryUrl,
        request: request
    };
})();
