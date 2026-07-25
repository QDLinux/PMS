(function () {
    var existing = window.PMFeatureFlags || {};
    window.PMFeatureFlags = Object.assign({
        dashboardMode: "vue",
        vueDashboardPath: "/app/index.html",
        legacyDashboardPath: "/dashboard.html",
        forceLegacyQueryKey: "legacyDashboard"
    }, existing);
})();
