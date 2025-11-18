
(function () {

    function modal(idOrEl, open) {
        const el = typeof idOrEl === "string" ? document.getElementById(idOrEl) : idOrEl;
        if (!el) return;
        if (open) {
            el.removeAttribute("hidden");
        } else {
            el.setAttribute("hidden", "hidden");
        }
    }
    window.modal = modal;

    function showToast(message, type) {
        if (!message) return;

        let toast = document.getElementById("toast");
        if (!toast) {
            toast = document.createElement("div");
            toast.id = "toast";
            toast.className = "toast";
            document.body.appendChild(toast);
        }

        toast.textContent = message;

        toast.classList.remove("toast-error", "toast-success", "show");
        if (type === "error") toast.classList.add("toast-error");
        if (type === "success") toast.classList.add("toast-success");

        void toast.offsetWidth;
        toast.classList.add("show");

        setTimeout(() => {
            toast.classList.remove("show");
        }, 3000);
    }
    window.showToast = showToast;

    document.addEventListener("DOMContentLoaded", function () {

        const loginBtns = document.querySelectorAll("#openLoginBtn");
        loginBtns.forEach(btn => {
            btn.addEventListener("click", () => modal("loginModal", true));
        });

        const registerBtns = document.querySelectorAll("#openRegisterBtn");
        registerBtns.forEach(btn => {
            btn.addEventListener("click", () => modal("registerModal", true));
        });

        document.querySelectorAll("[data-close]").forEach(btn => {
            btn.addEventListener("click", () => {
                const sel = btn.getAttribute("data-close");
                if (!sel) return;
                const target = document.querySelector(sel);
                if (target) modal(target, false);
            });
        });

        const createOverlay = document.getElementById("createOverlay");
        const openCreateBtn = document.getElementById("openCreatePanelBtn");
        const closeCreateBtn = document.getElementById("closeCreatePanelBtn");
        const createBackdrop = document.getElementById("createBackdrop");

        function openCreatePanel() {
            if (createOverlay) createOverlay.removeAttribute("hidden");
        }

        function closeCreatePanel() {
            if (createOverlay) createOverlay.setAttribute("hidden", "hidden");
        }

        if (openCreateBtn) {
            openCreateBtn.addEventListener("click", () => {
                const loggedIn = openCreateBtn.dataset.loggedIn === "true";
                if (!loggedIn) {
                    showToast("로그인 후 모임을 만들 수 있습니다.", "error");
                   // modal("loginModal", true);  //로그인창 바로 띄울거면 주석 제거하면 됨

                    return;
                }
                openCreatePanel();
            });
        }
        if (closeCreateBtn) {
            closeCreateBtn.addEventListener("click", closeCreatePanel);
        }
        if (createBackdrop) {
            createBackdrop.addEventListener("click", closeCreatePanel);
        }

        const imageInput = document.getElementById("imageFile");
        const imagePreview = document.getElementById("imagePreview");

        if (imageInput && imagePreview) {
            imageInput.addEventListener("change", () => {
                const file = imageInput.files && imageInput.files[0];
                if (!file) {
                    imagePreview.textContent = "이미지 미리보기";
                    imagePreview.style.backgroundImage = "";
                    return;
                }
                const reader = new FileReader();
                reader.onload = (e) => {
                    imagePreview.style.backgroundImage = `url('${e.target.result}')`;
                    imagePreview.style.backgroundSize = "cover";
                    imagePreview.style.backgroundPosition = "center center";
                    imagePreview.textContent = "";
                };
                reader.readAsDataURL(file);
            });
        }

        const newCategoryInput = document.getElementById("newCategoryInput");
        const addCategoryBtn = document.getElementById("addCategoryBtn");
        const newCategoryChips = document.getElementById("newCategoryChips");
        const newCategoryHidden = document.getElementById("newCategoryHidden");

        async function createCategories(namesText) {
            const trimmed = (namesText || "").trim();
            if (!trimmed) {
                showToast("추가할 카테고리 이름을 입력해 주세요.", "error");
                return;
            }

            try {
                const res = await fetch("/api/categories", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({ name: trimmed })
                });

                if (!res.ok) {
                    showToast("카테고리를 생성하는 데 실패했습니다.", "error");
                    return;
                }

                const list = await res.json(); // [{id, name}, ...]
                if (!Array.isArray(list)) return;

                list.forEach(cat => {
                    if (!cat || cat.id == null) return;

                    if (newCategoryHidden &&
                        newCategoryHidden.querySelector(`input[type="hidden"][value="${cat.id}"]`)) {
                        return;
                    }

                    // chip 표시
                    if (newCategoryChips) {
                        const chip = document.createElement("span");
                        chip.className = "chip";
                        chip.textContent = "#" + cat.name;
                        newCategoryChips.appendChild(chip);
                    }

                    // hidden input 추가 (form 전송용)
                    if (newCategoryHidden) {
                        const hidden = document.createElement("input");
                        hidden.type = "hidden";
                        hidden.name = "categoryIds";
                        hidden.value = cat.id;
                        newCategoryHidden.appendChild(hidden);
                    }
                });

                // 성공했으면 살짝만 안내 (원하면 이 줄 지워도 됨)
                showToast("카테고리가 추가되었습니다.", "success");

            } catch (e) {
                console.error(e);
                showToast("카테고리 생성 중 오류가 발생했습니다.", "error");
            }
        }

        if (addCategoryBtn && newCategoryInput && newCategoryHidden && newCategoryChips) {
            addCategoryBtn.addEventListener("click", async () => {
                await createCategories(newCategoryInput.value);
                newCategoryInput.value = "";
                newCategoryInput.focus();
            });

            // Enter 키로도 추가 가능하게
            newCategoryInput.addEventListener("keydown", async (e) => {
                if (e.key === "Enter") {
                    e.preventDefault();
                    await createCategories(newCategoryInput.value);
                    newCategoryInput.value = "";
                }
            });
        }
    });

    function setupLoginIdCheck(inputId, buttonId, helpId) {
        const input = document.getElementById(inputId);
        const btn = document.getElementById(buttonId);
        const help = document.getElementById(helpId);
        if (!input || !btn || !help) return;

        btn.addEventListener("click", async () => {
            const v = (input.value || "").trim();
            if (!v) {
                showToast("아이디를 입력해 주세요.", "error");
                input.focus();
                return;
            }

            try {
                // EmailApiController @RequestMapping("/api/auth") 기준
                const res = await fetch(`/api/auth/check-login-id?loginId=${encodeURIComponent(v)}`);

                if (!res.ok) {
                    const errText = await res.text();
                    help.textContent = errText;
                    help.style.color = "#f88";
                    showToast(errText, "error");
                    return;
                }

                const data = await res.json(); // {loginId, available}
                if (data.available) {
                    help.textContent = "사용 가능한 아이디입니다.";
                    help.style.color = "#1db954";
                    showToast("사용 가능한 아이디입니다.", "success");
                } else {
                    help.textContent = "이미 사용 중인 아이디입니다.";
                    help.style.color = "#f88";
                    showToast("이미 사용 중인 아이디입니다.", "error");
                }
            } catch (e) {
                console.error(e);
                showToast("아이디 중복 검사 중 오류가 발생했습니다.", "error");
            }
        });
    }

    setupLoginIdCheck("regLoginIdPage", "btnCheckLoginIdPage", "loginIdHelpPage");
    setupLoginIdCheck("regLoginIdModal", "btnCheckLoginIdModal", "loginIdHelpModal");

    function setupEmailVerification(config) {
        const emailInput = document.getElementById(config.emailInputId);
        const codeInput = document.getElementById(config.codeInputId);
        const sendBtn = document.getElementById(config.sendButtonId);
        const verifyBtn = document.getElementById(config.verifyButtonId);
        const help = document.getElementById(config.helpTextId);
        const verifiedHidden = document.getElementById(config.verifiedHiddenId);

        if (!emailInput || !codeInput || !sendBtn || !verifyBtn || !help || !verifiedHidden) {
            return;
        }

        let timerId = null;
        let remainSeconds = 0;

        function updateButtonText() {
            if (remainSeconds > 0) {
                sendBtn.textContent = `재전송 (${remainSeconds}s)`;
                sendBtn.disabled = true;
            } else {
                sendBtn.textContent = "인증코드 보내기";
                sendBtn.disabled = false;
            }
        }

        function startCooldown(sec) {
            remainSeconds = sec;
            updateButtonText();
            if (timerId) clearInterval(timerId);
            timerId = setInterval(() => {
                remainSeconds--;
                if (remainSeconds <= 0) {
                    clearInterval(timerId);
                    timerId = null;
                }
                updateButtonText();
            }, 1000);
        }

        sendBtn.addEventListener("click", async () => {
            const email = (emailInput.value || "").trim();
            if (!email) {
                showToast("이메일을 입력해 주세요.", "error");
                emailInput.focus();
                return;
            }
            try {
                const res = await fetch("/api/auth/email/send", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ email })
                });

                const text = await res.text();

                if (!res.ok) {
                    help.textContent = text;
                    help.style.color = "#f88";
                    showToast(text, "error");
                    return;
                }

                help.textContent = "인증 코드가 전송되었습니다. 메일을 확인해 주세요.";
                help.style.color = "#1db954";
                verifiedHidden.value = "false"; // 새로 보냈으니 다시 false
                showToast(text || "인증 코드를 전송했습니다.", "success");

                // 프론트에서도 60초 쿨타임
                startCooldown(60);
            } catch (e) {
                console.error(e);
                showToast("인증 메일 전송 중 오류가 발생했습니다.", "error");
            }
        });

        verifyBtn.addEventListener("click", async () => {
            const email = (emailInput.value || "").trim();
            const code = (codeInput.value || "").trim();

            if (!email || !code) {
                showToast("이메일과 인증코드를 모두 입력해 주세요.", "error");
                return;
            }

            try {
                const res = await fetch("/api/auth/email/verify", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ email, code })
                });
                const text = await res.text();

                if (!res.ok) {
                    help.textContent = text;
                    help.style.color = "#f88";
                    verifiedHidden.value = "false";
                    showToast(text, "error");
                    return;
                }

                help.textContent = "이메일 인증이 완료되었습니다.";
                help.style.color = "#1db954";
                verifiedHidden.value = "true";
                showToast(text || "이메일 인증이 완료되었습니다.", "success");
            } catch (e) {
                console.error(e);
                showToast("인증 코드 검증 중 오류가 발생했습니다.", "error");
            }
        });

        const form = emailInput.closest("form");
        if (form) {
            form.addEventListener("submit", (e) => {
                if (verifiedHidden.value !== "true") {
                    e.preventDefault();
                    showToast("이메일 인증을 완료해 주세요.", "error");
                }
            });
        }
    }

    setupEmailVerification({
        emailInputId: "regEmailPage",
        codeInputId: "regEmailCodePage",
        sendButtonId: "btnSendEmailCodePage",
        verifyButtonId: "btnVerifyEmailCodePage",
        helpTextId: "emailHelpPage",
        verifiedHiddenId: "emailVerifiedPage"
    });

    setupEmailVerification({
        emailInputId: "regEmailModal",
        codeInputId: "regEmailCodeModal",
        sendButtonId: "btnSendEmailCodeModal",
        verifyButtonId: "btnVerifyEmailCodeModal",
        helpTextId: "emailHelpModal",
        verifiedHiddenId: "emailVerifiedModal"
    });
})();
