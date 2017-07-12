# Contributing Guidelines

Thanks for your interest in opsu! This document provides basic guidelines for
contributing to the repository. If you plan on making major changes, consider
[opening an issue][issues] first to discuss your ideas.

[issues]: https://github.com/itdelatrisu/opsu/issues/new

## Making a Change

1. [Fork the repository][fork] and set up your build environment, as described
   in the [README][buildenv].
2. Make your desired code changes in your fork.
3. Test your change. There are no automated tests, so just do this manually.
   Read the [testing tips](#testing-tips) below for some suggestions.
4. Commit your change and create a [pull request][PR]. Follow up with any
   requested changes as needed.

[fork]: https://help.github.com/articles/fork-a-repo/
[buildenv]: README.md#building
[PR]: https://help.github.com/articles/creating-a-pull-request-from-a-fork/

## Guidelines

* A pull request should only contain one feature or bug fix. If you want to make
  multiple changes, create branches and open separate pull requests.
* Don't change more than you need to. In particular, don't change the coding
  style or move existing blocks around.
* In general, follow the same coding style as the file that you're editing.
* Write comments in your code as needed. At minimum, [Javadoc][Javadoc] comments
  are expected on all classes, methods, and global variables.

[Javadoc]: https://en.wikipedia.org/wiki/Javadoc#Technical_architecture

## Coding Style

* Use tabs for indenting, not spaces.
* Indentation and brace placement follow [Java conventions][indent]. Braces are
  usually not used for single-statement `if`, `while`, and `for`.
* There is no maximum line length; break long lines (or not) for readability.
* For now, avoid using features of Java 8 or higher (e.g. streams, lambdas).

[indent]: https://en.wikipedia.org/wiki/Indent_style#Variant:_Java

## Testing Tips

* **Gameplay changes:** Depending on the change, consider playing through a
  regular or [2B][2B] beatmap, watching a replay, pausing/resuming the game,
  enabling/disabling experimental sliders, etc.
* **UI changes:** Be sure to try different client resolutions (such as
  800x600 and widescreen) and different skins (if applicable).
* **Graphics/audio changes:** Test on different operating systems if you can,
  especially if your change could break Linux audio in any way.

[2B]: https://osu.ppy.sh/s/90935
