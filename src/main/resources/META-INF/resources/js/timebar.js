window.addEventListener('load', function () {
    document.querySelectorAll('[data-timebar]').forEach(function (timebar) {
        let timebarWidth = timebar.offsetWidth;
        let timebarHeight = parseInt(timebar.getAttribute('data-timebar-height')) || 100;
        timebar.style.position = 'relative';

        let controllLayer = document.createElement('div');
        controllLayer.style.position = 'absolute';
        controllLayer.style.top = '0';
        controllLayer.style.left = '0';
        controllLayer.style.width = timebarWidth + 'px';
        controllLayer.style.height = timebarHeight - 3 + 'px';

        timebar.appendChild(controllLayer);

        const selectorRadius = 7;
        const searchField = timebar.getAttribute('data-search-field') || "publish.date.range";

        let selectorStart = document.createElement('div');
        selectorStart.style.position = 'absolute';
        selectorStart.style.bottom = -(selectorRadius * 2) - 2 + 'px';
        selectorStart.style.left = -selectorRadius + 'px';
        selectorStart.style.borderRadius = '50% 50% 50% 0';
        selectorStart.style.transform = 'rotate(130deg)';
        selectorStart.style.width = (selectorRadius * 2) + 'px';
        selectorStart.style.height = (selectorRadius * 2) + 'px';
        selectorStart.style.cursor = 'grab';
        selectorStart.classList.add('bg-primary');
        selectorStart.classList.add('selector-start');
        controllLayer.appendChild(selectorStart);

        let selectorEnd = document.createElement('div');
        selectorEnd.style.position = 'absolute';
        selectorEnd.style.bottom = -(selectorRadius * 2) - 2 + 'px';
        selectorEnd.style.right = -selectorRadius + 'px';
        selectorEnd.style.borderRadius = '50% 50% 50% 0';
        selectorEnd.style.transform = 'rotate(130deg)';
        selectorEnd.style.width = (selectorRadius * 2) + 'px';
        selectorEnd.style.height = (selectorRadius * 2) + 'px';
        selectorEnd.style.cursor = 'grab';
        selectorEnd.classList.add('bg-primary');
        selectorEnd.classList.add('selector-end');
        controllLayer.appendChild(selectorEnd);

        let canvas = document.createElement('canvas');
        timebar.appendChild(canvas);
        const context = canvas.getContext('2d');

        let data = [];
        let maxX = 0;
        let maxY = 0;
        let minX = Number.MAX_VALUE;
        let minY = Number.MAX_VALUE;
        let xRange;
        let yRange;
        let dataNormalized;
        let selectorXStart = 0;
        let selectorXEnd = 0;

        let formInputGroup = document.createElement('div');
        formInputGroup.classList.add('input-group');
        formInputGroup.classList.add('input-group-sm');
        formInputGroup.classList.add('mt-3');

        let formStart = document.createElement('input');
        formStart.type = 'text';
        formStart.classList.add("form-control");
        formStart.classList.add("form-control-sm");
        formStart.classList.add("selector-start-form");
        formStart.placeholder = 'JJJJ';
        formInputGroup.appendChild(formStart);

        let formInputGroupAppendOne = document.createElement('div');
        formInputGroupAppendOne.classList.add('input-group-append');

        let formInputGroupText = document.createElement('span');
        formInputGroupText.classList.add('input-group-text');
        formInputGroupText.innerHTML = ' - ';
        formInputGroupAppendOne.appendChild(formInputGroupText);
        formInputGroup.appendChild(formInputGroupAppendOne);

        let formEnd = document.createElement('input');
        formEnd.type = 'text';
        formEnd.classList.add("form-control");
        formEnd.classList.add("form-control-sm");
        formEnd.classList.add("selector-end-form");
        formEnd.placeholder = 'JJJJ';
        formInputGroup.appendChild(formEnd);
        timebar.appendChild(formInputGroup);

        let formInputGroupAppendTwo = document.createElement('div');
        formInputGroupAppendTwo.classList.add('input-group-append');
        formInputGroup.appendChild(formInputGroupAppendTwo);

        let buttonApply = document.createElement('button');
        buttonApply.classList.add('btn');
        buttonApply.classList.add('btn-sm');
        buttonApply.classList.add('btn-secondary');
        buttonApply.innerHTML = '<i class="fas fa-check"></i>';
        formInputGroupAppendTwo.appendChild(buttonApply);

        let buttonReset = document.createElement('button');
        buttonReset.classList.add('btn');
        buttonReset.classList.add('btn-sm');
        buttonReset.classList.add('btn-danger');
        buttonReset.innerHTML = '<i class="fas fa-times"></i>';

        const urlSearchParams = new URLSearchParams(location.search);
        const hasRange = urlSearchParams.getAll('fq').filter(function (value) {
            return value.startsWith(searchField + ':');
        }).length>0;
        if(hasRange) {
            formInputGroupAppendTwo.appendChild(buttonReset);
            buttonReset.addEventListener('click', function () {
                let keep = [];
                let doDelete = false;
                urlSearchParams.getAll('fq').forEach(function (value) {
                    if (!value.startsWith(searchField + ':')) {
                        keep.push(value);
                    } else {
                        doDelete = true;
                    }
                });
                if (doDelete) {
                    urlSearchParams.delete('fq');
                    keep.forEach(function (value) {
                        urlSearchParams.append("fq", value);
                    });
                }
                location.search = urlSearchParams.toString();
            });
        }

        let tooltip = document.createElement('div');
        tooltip.classList.add('tb-tooltip');
        tooltip.classList.add('tb-tooltip-top');
        tooltip.classList.add('text-primary');
        tooltip.style.position = 'absolute';
        tooltip.style.display = 'none';
        tooltip.style.zIndex = '1000';
        tooltip.style.backgroundColor = 'rgba(0, 0, 0, 0.7)';
        tooltip.style.padding = '5px';
        tooltip.style.pointerEvents = 'none';
        tooltip.textContent = '0';
        tooltip.style.borderRadius = '10px';
        tooltip.style.borderBottomLeftRadius = '0';
        tooltip.style.whiteSpace = 'nowrap';
        timebar.appendChild(tooltip);


        function getSelectorOnePos() {
            let posFromLeft = parseInt(selectorStart.style.left.replace("px", ""));
            posFromLeft = posFromLeft + selectorRadius;
            return posFromLeft;
        }

        function setSelectorOnePos(posFromLeft) {
            let pos = posFromLeft - selectorRadius;
            selectorStart.style.left = pos + 'px';
        }

        function getSelectorTwoPos() {
            let posFromRight = parseInt(selectorEnd.style.right.replace("px", ""));
            posFromRight = posFromRight + selectorRadius;
            return posFromRight;
        }

        function setSelectorTwoPos(posFromRight) {
            let pos = posFromRight - selectorRadius;
            selectorEnd.style.right = pos + 'px';
        }

        function updateGradient() {
            const selectorOnePos = getSelectorOnePos();
            const selectorTwoPos = getSelectorTwoPos();
            const twoPos = canvas.clientWidth - selectorTwoPos;

            const startPos = "rgba(0,0,0,0.30) 0%";
            const changeOne = "rgba(0,0,0,0.30) " + (selectorOnePos) + "px";
            const changeTwo = "rgba(0,9,11,0) " + (selectorOnePos) + "px";
            const changeThree = "rgba(0,9,11,0) " + twoPos + "px";
            const changeFour = "rgba(0,0,0,0.30) " + twoPos + "px";
            const changeFive = "rgba(0,0,0,0.30) 100%";

            controllLayer.style.background = "linear-gradient(90deg," + startPos + ", " + changeOne + ", " + changeTwo + ", " + changeThree + ", " + changeFour + ", " + changeFive + ")";
        }

        function parseData() {
            for (let i = 0; i < data.length; i++) {
                const element = data[i];
                const x = element[0];
                const y = element[1];
                if (x > maxX) {
                    maxX = x;
                }
                if (x < minX) {
                    minX = x;
                }
                if (y > maxY) {
                    maxY = y;
                }
                if (y < minY) {
                    minY = y;
                }
            }

            data.sort(function (a, b) {
                return a[0] - b[0];
            });
            xRange = maxX - minX;
            yRange = maxY - minY;

            selectorXStart = 0;
            selectorXEnd = xRange;

            dataNormalized = [];
            for (let x = 0; x < xRange; x++) {
                dataNormalized.push(0);
            }
            for (let i = 0; i < data.length; i++) {
                dataNormalized[data[i][0] - minX] = data[i][1];
            }

            if (timebar.getAttribute('data-timebar-log') === 'true') {
                let yFac = maxY / Math.log(maxY);
                for (let i = 0; i < dataNormalized.length; i++) {
                    dataNormalized[i] = Math.log(dataNormalized[i] + 1) * yFac;
                }
            }
        }

        function drawData() {
            const xScale = canvas.clientWidth / dataNormalized.length * 2;
            const yScale = canvas.clientHeight / maxY;

            canvas.width = dataNormalized.length * xScale;
            canvas.height = maxY * yScale;

            context.scale(xScale, -1 * yScale);
            context.translate(0, -maxY);
            canvas.style.width = "100%";
            canvas.style.height = timebarHeight - 3 + 'px';

            context.fillStyle =  window.getComputedStyle(buttonApply).backgroundColor;


            context.beginPath()

            for (let x = 0; x < dataNormalized.length; x++) {
                const y = dataNormalized[x];
                if (x === 0) {
                    context.moveTo(x, 0);

                } else {
                    context.lineTo(x, 0);
                }
                context.lineTo(x, y);
                context.lineTo(x + 1, y);
                context.lineTo(x + 1, 0);
            }
            context.lineTo((dataNormalized.length - 1), -1);
            context.lineTo(-1, -1);
            context.fill();

            context.fillStyle = "rgba(255,255,255,0.1)";


            context.globalCompositeOperation = "lighten";
            context.beginPath();
            for (let x = 1; x < dataNormalized.length; x+=2) {
                const y = dataNormalized[x];
                if (x === 0) {
                    context.moveTo(x, 0);

                } else {
                    context.lineTo(x, 0);
                }
                context.lineTo(x, y);
                context.lineTo(x + 1, y);
                context.lineTo(x + 1, 0);
            }
            context.lineTo((dataNormalized.length - 1), -1);
            context.lineTo(-1, -1);
            context.fill();
        }

        function updateForms() {
            const selectorOnePos = getSelectorOnePos();
            const selectorTwoPos = getSelectorTwoPos();
            const twoPos = canvas.clientWidth - selectorTwoPos;


            formStart.value = Math.max(minX, Math.floor(minX + ((selectorOnePos * dataNormalized.length - 1) / canvas.clientWidth))).toString();
            formEnd.value = Math.min(maxX, Math.ceil(minX + (twoPos * dataNormalized.length - 1) / canvas.clientWidth) - 1).toString();
        }


        function validateForm() {
            if (data.length === 0) {
                return;
            }
            let startFormValue = parseInt(formStart.value);
            let endFormValue = parseInt(formEnd.value);

            if (isNaN(startFormValue)) {
                formStart.classList.add('is-invalid');
                return false;
            }

            if (isNaN(endFormValue)) {
                formEnd.classList.add('is-invalid');
                return false;
            }

            if (endFormValue < startFormValue) {
                formEnd.classList.add('is-invalid');
                return false;
            }

            if (startFormValue < minX || startFormValue > maxX) {
                formStart.classList.add('is-invalid');
                return false;
            }

            if (endFormValue < minX || endFormValue > maxX) {
                formEnd.classList.add('is-invalid');
                return false;
            }

            formEnd.classList.remove('is-invalid');
            formStart.classList.remove('is-invalid');
            setSelectorOnePos(((startFormValue - minX)) / (xRange + 1) * canvas.clientWidth + 1);
            setSelectorTwoPos((((maxX - endFormValue))) / (xRange + 1) * canvas.clientWidth + 1);

            updateGradient();
            return true;
        }

        let isDraggingOne = false;
        let isDraggingTwo = false;
        let dragClient = 0;
        let startPosX = 0;

        function mouseMove(e) {
            e.preventDefault();
            const posX = e.clientX;
            if (isDraggingOne) {
                setSelectorOnePos(Math.min(Math.max(startPosX + posX - dragClient, 0), canvas.clientWidth - selectorRadius - getSelectorTwoPos()));
            } else if (isDraggingTwo) {
                setSelectorTwoPos(Math.min(Math.max(dragClient - (posX - startPosX), 0), canvas.clientWidth - selectorRadius - getSelectorOnePos()));
            }

            updateGradient();
            updateForms();

            return false;
        }

        function addDragEndMoveListeners() {
            document.addEventListener('mousemove', mouseMove);
            const mouseUp = function () {
                if (isDraggingOne || isDraggingTwo) {
                    isDraggingOne = false;
                    isDraggingTwo = false;
                }
                document.removeEventListener('mousemove', mouseMove);
                document.removeEventListener('mouseup', mouseUp);
                selectorStart.style.cursor = 'grab';
                selectorEnd.style.cursor = 'grab';
            };
            document.addEventListener('mouseup', mouseUp);
        }

        selectorStart.addEventListener('mousedown', function (e) {
            if (isDraggingOne || isDraggingTwo) {
                return;
            }
            isDraggingOne = true;
            dragClient = e.clientX;
            startPosX = getSelectorOnePos();

            addDragEndMoveListeners();
            selectorStart.style.cursor = 'grabbing';
            return false;
        });


        selectorEnd.addEventListener('mousedown', function (e) {
            if (isDraggingOne || isDraggingTwo) {
                return;
            }
            isDraggingTwo = true;
            dragClient = e.clientX;
            startPosX = getSelectorTwoPos();

            addDragEndMoveListeners();
            selectorEnd.style.cursor = 'grabbing';
            return false;
        });

        formStart.addEventListener('change', function () {
            validateForm();
        });


        formEnd.addEventListener('change', function () {
            validateForm();
        });

        buttonApply.addEventListener('click', function () {
            if (data.length === 0) {
                return;
            }
            const urlSearchParams = new URLSearchParams(location.search);

            if (!validateForm()) {
                return;
            }

            let startFormValue = parseInt(formStart.value);
            let endFormValue = parseInt(formEnd.value);
            let keep = [];
            let doDelete = false;
            urlSearchParams.getAll('fq').forEach(function (value) {
                if (!value.startsWith(searchField + ':')) {
                    keep.push(value);
                } else {
                    doDelete = true;
                }
            });
            if (doDelete) {
                urlSearchParams.delete('fq');
                keep.forEach(function (value) {
                    urlSearchParams.append("fq", value);
                });
            }
            urlSearchParams.append('fq', searchField + ":[" + startFormValue + "-01-01 TO " + endFormValue + "-12-31]");
            location.search = urlSearchParams.toString();
        });

        controllLayer.addEventListener('mouseenter', function () {
            tooltip.style.display = 'block';
        });

        controllLayer.addEventListener('mouseleave', function () {
            tooltip.style.display = 'none';
        });

        controllLayer.addEventListener('mousemove', function (e) {
            if (data.length === 0) {
                return;
            }
            const posX = e.clientX - canvas.getBoundingClientRect().left;
            // const posY = e.clientY - canvas.getBoundingClientRect().top;

            const index = Math.floor(((posX * dataNormalized.length - 1) / canvas.clientWidth));
            const bounds = Math.min(Math.max(0, index), xRange);
            const year = minX + bounds;
            const value = dataNormalized[bounds];

            tooltip.style.left = posX + 'px';
            tooltip.style.top = canvas.clientHeight - (canvas.height / maxY) * value * (canvas.clientHeight / canvas.height) - tooltip.clientHeight + 'px';


            tooltip.textContent = year.toString() + ": " + value.toString();
        });

        function parseURLData() {
            const urlSearchParams = new URLSearchParams(location.search);
            urlSearchParams.getAll('fq').forEach(function (value) {
                if (value.startsWith(searchField + ':')) {
                    const groups = /.+:\[(?<start>\d{0,4})-\d{0,2}-\d{0,2} TO (?<end>\d{0,4})-\d{0,2}-\d{0,2}]/.exec(value).groups;
                    formStart.value = groups.start;
                    formEnd.value = groups.end;

                    if (validateForm()) {
                        updateGradient();
                    }
                }
            });
        }

        function loadData() {
            const dataTimebar = timebar.getAttribute("data-timebar");
            if (dataTimebar !== "true") {
                data = JSON.parse(dataTimebar);
                parseData();
                drawData();
                parseURLData();
            } else {
                const urlSearchParams = new URLSearchParams(location.search);
                let keep = [];
                let doDelete = false;
                urlSearchParams.getAll('fq').forEach(function (value) {
                    if (!value.startsWith(searchField + ':')) {
                        keep.push(value);
                    } else {
                        doDelete = true;
                    }
                });
                if (doDelete) {
                    urlSearchParams.delete('fq');
                    keep.forEach(function (value) {
                        urlSearchParams.append("fq", value);
                    });
                }
                urlSearchParams.append("XSL.Style", "xml");
                if(urlSearchParams.has("rows")) {
                    urlSearchParams.delete("rows");
                }
                urlSearchParams.append("rows", "0");
                urlSearchParams.append("facet", "true");
                urlSearchParams.append("facet.range", searchField);
                urlSearchParams.append("facet.range.start", timebar.getAttribute("data-timebar-start") || "0001-01-01T00:00:00Z");
                urlSearchParams.append("facet.range.end", timebar.getAttribute("data-timebar-end") || "NOW");
                urlSearchParams.append("facet.range.gap", timebar.getAttribute("data-timebar-gap") || "+1YEAR");
                urlSearchParams.append("facet.mincount", timebar.getAttribute("data-timebar-mincount") || "1");

                const request = new XMLHttpRequest();
                request.open("GET", location.origin + location.pathname + "?" + urlSearchParams.toString(), true);

                request.addEventListener('readystatechange', function () {
                    if (this.readyState === 4 && this.status === 200) {
                        const xml = this.responseXML;
                        const result = xml.evaluate('//lst[@name="facet_ranges"]/lst[@name="' + searchField + '"]/lst[@name="counts"]', xml, null);
                        let node = result.iterateNext();
                        node.childNodes.forEach(function (value) {
                            if (value.nodeType === Node.ELEMENT_NODE) {
                                const yearStr = value.getAttribute('name');
                                const year = parseInt(yearStr.split('-')[0]);
                                const count = parseInt(value.textContent);
                                data.push([year, count]);
                            }
                        });

                        parseData();
                        drawData();
                        parseURLData();
                    }

                });
                request.send();
            }
        }

        loadData();
    });
})
