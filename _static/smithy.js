$(function() {
    // Scroll spy to change highlighted navigation element.
    const scrollSpy = function() {
        const section = document.querySelectorAll(".section");
        const sections = {};
        Array.prototype.forEach.call(section, function (e) {
            sections[e.id] = e.offsetTop;
        });
        return function() {
            const scrollPosition = document.documentElement.scrollTop || document.body.scrollTop;
            for (let i in sections) {
                if (sections[i] <= scrollPosition) {
                    $('#right-column .current').removeClass('current');
                    $("#right-column a[href='#" + i + "']").addClass('current');
                }
            }
        };
    }();

    $(window).scroll(scrollSpy);
    scrollSpy();
});
