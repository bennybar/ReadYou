package me.ash.reader.ui.component.webview

object WebViewScript {

    fun get(boldCharacters: Boolean) = """
// Set each block's direction from the script most of its text is written in: the same rule as
// String.isRtl() uses for the lists. Not first-strong (what dir="auto" does), because a Hebrew
// paragraph routinely opens with a Latin brand and would lay out left-to-right on one word.
(function () {
    var RTL = /[֐-ࣿיִ-﷿ﹰ-﻿]/;
    var LTR = /[A-Za-zÀ-ɏͰ-ϿЀ-ӿ]/;

    function direction(text) {
        var rtl = 0, ltr = 0, first = null;
        for (var i = 0; i < text.length; i++) {
            var ch = text[i];
            if (RTL.test(ch)) { rtl++; if (!first) { first = 'rtl'; } }
            else if (LTR.test(ch)) { ltr++; if (!first) { first = 'ltr'; } }
        }
        if (rtl > ltr) { return 'rtl'; }
        if (ltr > rtl) { return 'ltr'; }
        return first;
    }

    // Throw away the direction the publisher asked for. Some feeds ship every block with
    // style="direction: ltr; text-align: left" (English-desk boilerplate on Hebrew copy), and an
    // inline style outranks a dir attribute, so the dir set below would be quietly overruled.
    var styled = document.querySelectorAll('article *');
    for (var s = 0; s < styled.length; s++) {
        styled[s].removeAttribute('align');
        if (styled[s].style) {
            styled[s].style.removeProperty('direction');
            styled[s].style.removeProperty('unicode-bidi');
            styled[s].style.removeProperty('text-align');
        }
    }

    // Leaf blocks only: dir on a wrapper would override the decisions of everything inside it.
    // `article` is included for content that arrives as bare text and <br>.
    var blocks = document.querySelectorAll(
        'article, p, li, blockquote, h1, h2, h3, h4, h5, h6, figcaption, td, th, div'
    );
    for (var i = 0; i < blocks.length; i++) {
        var block = blocks[i];
        if (block.querySelector('p, li, blockquote, h1, h2, h3, h4, h5, h6')) { continue; }
        var resolved = direction(block.textContent || '');
        if (resolved) { block.setAttribute('dir', resolved); }
    }

    // Lists and quotes are the exception: a <ul> always contains <li> and a quote usually
    // contains <p>, so they would always be skipped. A list's markers would then sit on one side
    // while its padding is reserved on the other, and a Hebrew quote's bar would be on the left.
    var lists = document.querySelectorAll('ul, ol, blockquote');
    for (var j = 0; j < lists.length; j++) {
        var resolvedList = direction(lists[j].textContent || '');
        if (resolvedList) { lists[j].setAttribute('dir', resolvedList); }
    }
})();

const BR_WORD_STEM_PERCENTAGE = 0.7;
const MAX_FIXATION_PARTS = 4;
const FIXATION_LOWER_BOUND = 0
function highlightText(sentenceText) {
	return sentenceText.replace(/\p{L}+/gu, (word) => {
		const { length } = word;

		const brWordStemWidth = length > 3 ? Math.round(length * BR_WORD_STEM_PERCENTAGE) : length;

		const firstHalf = word.slice(0, brWordStemWidth);
		const secondHalf = word.slice(brWordStemWidth);
		var htmlWord = "<br-bold>";
        htmlWord += makeFixations(firstHalf);
        htmlWord += "</br-bold>";
        if (secondHalf.length) {
            htmlWord += "<br-edge>";
            htmlWord += makeFixations(secondHalf);
            htmlWord += "</br-edge>";
        }
		return htmlWord;
	});
}

function makeFixations(textContent) {
	const COMPUTED_MAX_FIXATION_PARTS = textContent.length >= MAX_FIXATION_PARTS ? MAX_FIXATION_PARTS : textContent.length;

	const fixationWidth = Math.ceil(textContent.length * (1 / COMPUTED_MAX_FIXATION_PARTS));

	if (fixationWidth === FIXATION_LOWER_BOUND) {
		return '<br-fixation fixation-strength="1">' + textContent + '</br-fixation>';
	}

	const fixationsSplits = new Array(COMPUTED_MAX_FIXATION_PARTS).fill(null).map((item, index) => {
		const wordStartBoundary = index * fixationWidth;
		const wordEndBoundary = wordStartBoundary + fixationWidth > textContent.length ? textContent.length : wordStartBoundary + fixationWidth;

		return `<br-fixation fixation-strength="` + (index + 1) + `">` + textContent.slice(wordStartBoundary, wordEndBoundary) + `</br-fixation>`;
	});

	return fixationsSplits.join('');
}

const IGNORE_NODE_TAGS = ['STYLE', 'SCRIPT', 'BR-SPAN', 'BR-FIXATION', 'BR-BOLD', 'BR-EDGE', 'SVG', 'INPUT', 'TEXTAREA'];
function parseNode(node) {
    if (!node?.parentElement?.tagName || IGNORE_NODE_TAGS.includes(node.parentElement.tagName)) {
        return;
    }
    
    if (node.nodeType === Node.TEXT_NODE && node.nodeValue.length) {
        try {
            const brSpan = document.createElement('br-span');
            brSpan.innerHTML = highlightText(node.nodeValue);
            if (brSpan.childElementCount === 0) return;
            node.parentElement.replaceChild(brSpan, node); // JiffyReader keeps the old element around, but we don't need it
        } catch (e) {
            console.error('Error parsing text node:', e);
        }
        return;
    }
    
    if (node.hasChildNodes()) [...node.childNodes].forEach(parseNode);
}

function setBold(enabled) {
    if (enabled) {
        document.body.setAttribute("br-mode", "on");
        [...document.body.childNodes].forEach(parseNode);
    } else {
        document.body.setAttribute("br-mode", "off");
    }
}

${if (boldCharacters) "setBold(true);" else ""}

var images = document.querySelectorAll("img");

images.forEach(function(img) {
    img.onload = function() {
        img.classList.add("loaded");
        console.log("Image width:", img.width, "px");
        if (img.width < 412) {
            img.classList.add("thin");
        }
    };

    img.onerror = function() {
        console.error("Failed to load image:", img.src);
    };
});
"""
}
