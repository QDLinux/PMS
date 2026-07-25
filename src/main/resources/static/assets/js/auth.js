(function () {
    const authApi = window.AppApi;
    const vue = window.Vue;
    const appRoot = document.getElementById("authApp");
    if (!authApi || !vue || !appRoot) return;

    const { createApp } = vue;

    createApp({
        data() {
            const savedUsername = authApi.getSavedUsername();
            const initialTab = (window.location.hash || "#login").replace("#", "") === "register" ? "register" : "login";
            return {
                activeTab: initialTab,
                message: "",
                messageType: "",
                loginSubmitting: false,
                registerSubmitting: false,
                loginForm: {
                    username: savedUsername || "",
                    password: "",
                    remember: !!savedUsername
                },
                registerForm: {
                    username: "",
                    password: "",
                    confirmPassword: ""
                }
            };
        },
        methods: {
            setMessage(text, type) {
                this.message = text || "";
                this.messageType = type || "";
            },
            switchTab(target) {
                this.activeTab = target === "register" ? "register" : "login";
                window.location.hash = this.activeTab;
            },
            async tryAutoLogin() {
                if (!authApi.getToken()) return;
                try {
                    await authApi.request("/api/dashboard/summary", { method: "GET" });
                    window.location.href = authApi.getDashboardEntryUrl();
                } catch (err) {
                    authApi.clearAuth();
                }
            },
            async onLogin() {
                if (this.loginSubmitting) return;
                this.loginSubmitting = true;
                this.setMessage("登录中...");
                try {
                    const data = await authApi.request("/api/auth/login", {
                        method: "POST",
                        body: JSON.stringify({
                            username: this.loginForm.username,
                            password: this.loginForm.password
                        })
                    });
                    authApi.setAuth(data.token, data.username, this.loginForm.remember, data.expiresIn);
                    this.setMessage("登录成功，正在跳转...", "success");
                    setTimeout(function () {
                        window.location.href = authApi.getDashboardEntryUrl();
                    }, 300);
                } catch (err) {
                    this.setMessage(err.message, "error");
                } finally {
                    this.loginSubmitting = false;
                }
            },
            async onRegister() {
                if (this.registerSubmitting) return;
                this.registerSubmitting = true;
                this.setMessage("注册中...");
                if (this.registerForm.password !== this.registerForm.confirmPassword) {
                    this.setMessage("两次输入的密码不一致", "error");
                    this.registerSubmitting = false;
                    return;
                }
                try {
                    await authApi.request("/api/auth/register", {
                        method: "POST",
                        body: JSON.stringify({
                            username: this.registerForm.username,
                            password: this.registerForm.password
                        })
                    });
                    this.registerForm = {
                        username: "",
                        password: "",
                        confirmPassword: ""
                    };
                    this.switchTab("login");
                    this.setMessage("注册成功，请使用上方登录表单登录", "success");
                } catch (err) {
                    this.setMessage(err.message, "error");
                } finally {
                    this.registerSubmitting = false;
                }
            }
        },
        mounted() {
            this.tryAutoLogin();
        }
    }).mount("#authApp");
})();
