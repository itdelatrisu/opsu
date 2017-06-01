# Contributing to opsu!

Thank you for your interest in opsu!. Although osu! is slowly becoming
[open source][ppy/osu], FenekAlfa once [famously said][notosu]:

> "IMO the biggest advantage of opsu! is NOT being osu!."

[ppy/osu]: https://github.com/ppy/osu
[notosu]: https://github.com/itdelatrisu/opsu/issues/43#issuecomment-102289842

**Issues**: https://github.com/itdelatrisu/opsu/issues

## Getting Started

If you have not already done so, [fork the repo][fork] and set up your build
environment. The Java Development Kit (JDK) 7 or higher is required, as well as
either [Maven][] or [Gradle][] for building. The commands for building the
project are outlined in the [README][buildenv].

[fork]: https://help.github.com/articles/fork-a-repo/
[Maven]: https://maven.apache.org/
[Gradle]: https://gradle.org/
[buildenv]: README#building

## Making a Change

1. Clone your fork and create a new branch with a descriptive name.
2. Make your desired changes to the code.
3. Build and run the project to test your change. There are currently no
   automated tests, so testing should just be done manually.

   **If your change affects beatmaps,** test it against regular sliders, as
   well as merging and non-merging experimental sliders. Test also against a
   regular map and a [2B map][2B].
4. Once you are happy with the change, commit it. Try to follow the guidelines
   for [good commit messages][commit].
5. Create a [pull request][]. Make changes if any are requested.

[2B]: https://osu.ppy.sh/s/90935
[commit]: https://chris.beams.io/posts/git-commit/
[pull request]: https://help.github.com/articles/creating-a-pull-request-from-a-fork/

## Coding Style

- Use tabs for indenting, not spaces.
- Indentation and brace placement follow [Java conventions][indent]. Braces are
  not used for single-statement `if`, `while` and `for`. `switch` blocks are
  not indented.
- Pad operators as well as keywords `if`, `while`, `for`, `switch`, etc.
- There is no hard maximum line length, but keep it readable.
- Do not use Java 8 features such as streams or lambdas; the target version is
  Java 7

**When in doubt, code in the same style as the file you are editing.**

[indent]: https://en.wikipedia.org/wiki/Indent_style#Variant:_Java

## Tips

- Don't change more than you need to. That is, don't change the style or move
  things around unless it is directly related to your change.
- Each feature or bug fix should be in separate pull requests. In other words,
  a pull request should only contain one feature or bug fix.
- Follow the style guide.
