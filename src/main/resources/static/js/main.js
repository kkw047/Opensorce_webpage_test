// 모달 열기/닫기 + 우측 패널 + 이미지 미리보기 + 카테고리 추가 버튼 UX
document.addEventListener('DOMContentLoaded', () => {
    const $ = s => document.querySelector(s);

    // 로그인/회원가입 모달
    const loginModal = $('#loginModal');
    const registerModal = $('#registerModal');

    $('#openLoginBtn')?.addEventListener('click', () => { loginModal.style.display = 'flex'; });
    $('#openRegisterBtn')?.addEventListener('click', () => { registerModal.style.display = 'flex'; });

    document.querySelectorAll('[data-close]').forEach(el => {
        el.addEventListener('click', () => {
            const id = el.getAttribute('data-close');
            const modal = document.querySelector(id);
            if (modal) modal.style.display = 'none';
        });
    });

    [loginModal, registerModal].forEach(m => {
        m?.addEventListener('click', (e) => { if (e.target === m) m.style.display = 'none'; });
    });

    // 우측 "모임 만들기" 패널
    const panel = $('#createPanel');
    $('#openCreatePanelBtn')?.addEventListener('click', () => panel.style.display = 'block');
    $('#closeCreatePanelBtn')?.addEventListener('click', () => panel.style.display = 'none');

    // 이미지 미리보기
    const fileInput = $('#imageFile');
    const preview = $('#imagePreview');
    fileInput?.addEventListener('change', () => {
        const f = fileInput.files?.[0];
        if (!f) { preview.innerHTML = '이미지 미리보기'; return; }
        const url = URL.createObjectURL(f);
        preview.innerHTML = `<img src="${url}" alt="">`;
    });

    // 새 카테고리 추가 UX (입력 칸 옆 버튼 → 체크박스 목록에 가짜 라벨 추가)
    const addBtn = $('#addCategoryBtn');
    addBtn?.addEventListener('click', () => {
        const input = document.querySelector('input[name="newCategoryName"]');
        const name = input?.value?.trim();
        if (!name) return alert('추가할 카테고리명을 입력하세요.');
        const list = input.closest('form').querySelectorAll('div[style*="max-height:200px"]')[0];
        const id = 'new-cat-' + Date.now();
        const label = document.createElement('label');
        label.style.display = 'flex';
        label.style.gap = '8px';
        label.style.alignItems = 'center';
        label.innerHTML = `<input type="checkbox" checked disabled><span>${name} (추가 예정)</span>`;
        list.prepend(label);
        // 실제 저장은 서버에서 newCategoryName으로 처리됨
    });

    // 좌측 카테고리 필터: 체크 후 버튼으로 GET 제출 (폼 그대로 사용)
    // 별도 코드 필요 없음
});
