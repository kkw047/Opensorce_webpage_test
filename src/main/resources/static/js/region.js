(function (w) {
    function opt(value, text) {
        const o = document.createElement('option');
        o.value = value; o.textContent = text;
        return o;
    }

    w.initRegionSelects = function (doSel, siSel) {
        const $do = document.querySelector(doSel);
        const $si = document.querySelector(siSel);

        // 도 목록 로드
        fetch('/api/regions/dos')
            .then(r => r.json())
            .then(dos => {
                $do.innerHTML = '';
                $do.appendChild(opt('', '전체 도'));
                dos.forEach(d => $do.appendChild(opt(d, d)));
                $do.disabled = false;
            });

        // 도 변경 → 시/군/구 로드
        $do.addEventListener('change', () => {
            const v = $do.value;
            $si.innerHTML = '';
            if (!v) {
                $si.disabled = true;
                return;
            }
            fetch('/api/regions/sis?do=' + encodeURIComponent(v))
                .then(r => r.json())
                .then(sis => {
                    $si.appendChild(opt('', '전체 시/군/구'));
                    sis.forEach(s => $si.appendChild(opt(s, s)));
                    $si.disabled = false;
                });
        });
    };
})(window);
