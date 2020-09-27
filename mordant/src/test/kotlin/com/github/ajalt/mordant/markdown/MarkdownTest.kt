package com.github.ajalt.mordant.markdown

import com.github.ajalt.mordant.AnsiColor.black
import com.github.ajalt.mordant.AnsiColor.brightWhite
import com.github.ajalt.mordant.AnsiLevel
import com.github.ajalt.mordant.AnsiStyle.*
import com.github.ajalt.mordant.Terminal
import io.kotest.matchers.shouldBe
import org.intellij.lang.annotations.Language
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.CompositeASTNode
import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser
import kotlin.test.Test

class MarkdownTest {
    @Test
    fun `test paragraphs`() = doTest("""
Paragraph one
wrapped line.

Paragraph 
two
wrapped
line.
""", """
Paragraph one wrapped line.

Paragraph two wrapped line.
""")

    @Test
    fun `test paragraphs wrapped`() = doTest("""
Paragraph one
wrapped line.

Paragraph two
wrapped line.
""", """
Paragraph
one wrapped
line.

Paragraph
two wrapped
line.
""", width = 11)

    @Test
    fun `test quotes`() = doTest("""
This paragraph
has some "double quotes"
and some 'single quotes'.
""", """
This paragraph has some "double quotes" and some 'single quotes'.
""")

    @Test
    fun `test emphasis`() = doTest("""
An *em span*.

An _em span_.

A **strong span**.

A __strong span__.

A ***strong em span***.

A ~~strikethrough span~~.
""", """
An ${italic("em span")}.

An ${italic("em span")}.

A ${bold("strong span")}.

A ${bold("strong span")}.

A ${(bold + italic)("strong em span")}.

A ${strikethrough("strikethrough span")}.
""")

    @Test
    fun `test unordered list`() = doTest("""
- line 1
- line 2a
  line 2b
- line 3
""", """
 • line 1
 • line 2a line 2b
 • line 3
""")

    @Test
    fun `test unordered list wrap`() = doTest("""
- line 1
- line 2a
  line 2b
- line 3
""", """
 • line 1
 • line 2a
   line 2b
 • line 3
""", width = 10)

    @Test
    fun `test ordered list`() = doTest("""
1. line 1
1. line 2a
   line 2b
1. line 3
""", """
 1. line 1
 2. line 2a line 2b
 3. line 3
""")

    @Test
    fun `test ordered list wrap`() = doTest("""
1. line 1
1. line 2a
  line 2b
1. line 3
""", """
 1. line 1
 2. line 2a
    line 2b
 3. line 3
""", width = 11)

    @Test
    fun `block quote`() = doTest("""
> line 1
>
> line 2
""", """
▎ line 1
▎
▎ line 2
""")

    @Test
    fun `various links`() = doTest("""
[a reference link][a link]

[a link]

[an inline link](example.com "with a title")

![an image](example.com "with a title")

<https://example.com/autolink>

www.example.com/url

<autolink@example.com>

[a link]: example.com
""", """
a reference link

a link

an inline link

an image

https://example.com/autolink

www.example.com/url

<autolink@example.com>


""")

    @Test
    fun `default html`() = doTest("""
<h1 align="center">
    <img src="example.svg">
    <p>text in html</p>
</h1>
""", """
""")

    @Test
    fun `show html`() = doTest("""
<h1 align="center">
    <img src="example.svg">
    <p>text in html</p>
</h1>
""", """
<h1 align="center">
    <img src="example.svg">
    <p>text in html</p>
</h1>
""", showHtml = true)

    @Test
    fun `horizontal rule`() = doTest("""
---
""", """
──────────
""", width = 10)

    @Test
    fun `header 1 empty`() = doTest("""
#
""", """


""", width = 19)

    @Test
    fun `header 1`() = doTest("""
# Header Text
""", """

═══ Header Text ═══

""", width = 19)

    @Test
    fun `header 2`() = doTest("""
## Header Text
""", """

─── Header Text ───

""", width = 19)

    @Test
    fun `header 3`() = doTest("""
### Header Text
""", """

    Header Text    

""", width = 19)

    @Test
    fun `header 4`() = doTest("""
#### Header Text
""", """

${bold("Header Text")}

""", width = 19)

    @Test
    fun `header 5`() = doTest("""
##### Header Text
""", """

${italic("Header Text")}

""", width = 19)

    @Test
    fun `header 6`() = doTest("""
###### Header Text
""", """

${dim("Header Text")}

""", width = 19)

    @Test
    fun `header trailing chars`() = doTest("""
# Header Text ##
""", """

═══ Header Text ═══

""", width = 19)

    @Test
    fun `setext h1`() = doTest("""
Header Text
===========
""", """

═══ Header Text ═══

""", width = 19)

    @Test
    fun `setext h2`() = doTest("""
  Header Text  
---
""", """

─── Header Text ───

""", width = 19)

    @Test
    fun `empty code span`() = doTest("""
An `` empty code span.
""", """
An `` empty code span.
""")

    @Test
    fun `code span`() = doTest("""
This is a `code    <br/>`   span.

So `is  this`
span.

A `span
with
a
line break`.
""", """
This is a ${(brightWhite on black)("code <br/>")} span.

So ${(brightWhite on black)("is this")} span.

A ${(brightWhite on black)("span with a line break")}.
""")

// Not supported by the parser yet
// https://spec.commonmark.org/0.29/#backslash-escapes
//    @Test
//    fun `backslash escapes`() = doTest("""
//\!\"\#\${'$'}\%\&\'\(\)\*\+\,\-\.\/\:\;\<\=\>\?\@\[\\\]\^\_\`\{\|\}\~
//""", """
//!"#${'$'}%&'()*+,-./:;&lt;=&gt;?@[\]^_`{|}~
//""")

    @Test
    fun `hard line breaks`() = doTest("""
A hard   
line break with spaces.

A hard\
  line break with a backslash.
  
A *hard   
line* break with emph.

Code spans `don't\
have` hard breaks.
""", """
A hard
line break with spaces.

A hard
line break with a backslash.

A ${italic("hard")}
${italic("line")} break with emph.

Code spans ${(brightWhite on black)("don't\\ have")} hard breaks.
""")

    // https://github.github.com/gfm/#example-205
    @Test
    fun `header only table`() = doTest("""
| abc | def |
| --- | --- |
""", """
┌─────┐
│ foo │
└─────┘
""")

    // https://github.github.com/gfm/#example-198
    @Test
    fun `simple table`() = doTest("""
 | foo | bar |
 | --- | --- |
 | baz | bim |
""", """
┌─────┬─────┐
│ foo │ bar │
├─────┼─────┤
│ baz │ bim │
└─────┴─────┘
""")

    // https://github.github.com/gfm/#example-204
    @Test
    fun `non-rectangular table`() = doTest("""
| abc | def |
| --- | --- |
| bar |
| bar | baz | boo |
""", """
┌─────┬─────┐
│ abc │ def │
├─────┼─────┘
│ bar │      
├─────┼─────┐
│ bar │ baz │
└─────┴─────┘
""")

    @Test
    fun `table alignment`() = doTest("""
| abc | def | ghi |
| :---: | ---: | :--- |
| bar | baz | quz |
| ...... | ...... | ...... |
""", """
┌────────┬────────┬────────┐
│  abc   │    def │ ghi    │
├────────┼────────┼────────┤
│  bar   │    baz │ quz    │
├────────┼────────┼────────┤
│ ...... │ ...... │ ...... │
└────────┴────────┴────────┘
""")

    private fun doTest(
            @Language("markdown") markdown: String,
            expected: String,
            width: Int = 79,
            showHtml: Boolean = false
    ) {
        try {
            val terminal = Terminal(level = AnsiLevel.TRUECOLOR, width = width)
            val actual = terminal.renderMarkdown(markdown, showHtml)
            try {
                actual shouldBe expected
            } catch (e: Throwable) {
                println(actual)
                throw e
            }
        } catch (e: Throwable) {
            // Print parse tree on test failure
            val flavour: MarkdownFlavourDescriptor = GFMFlavourDescriptor()
            val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(markdown)
            println(parsedTree.children.joinToString("\n"))
            throw e
        }
    }
}
