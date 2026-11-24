# BrailleBlaster

## A brief history of BrailleBlaster

BrailleBlaster grew out of a problem John J. Boyer knew firsthand. As a
blind student interested in science, he had found very little scientific
material available in braille. Boyer went on to lead the development of
Liblouis, an open-source braille translation library. In July 2010, he
announced BrailleBlaster as a cross-platform project for translating and
formatting braille documents, with particular attention to math books and
tactile graphics. He named ViewPlus Technologies and his company,
Abilitiessoft, as sponsors and invited transcribers, technical writers,
and programmers to contribute.
[2010 project announcement][announcement] · [2012 Boyer profile][boyer]

The American Printing House for the Blind (APH) took over development by
2014 and announced the first public beta in November 2016. APH focused on
getting braille textbooks into students' hands by the first day of class.
BrailleBlaster used Liblouis to translate text and could use the structure
of publisher files, such as NIMAS and EPUB, to automate parts of
formatting. Transcribers could generate tables of contents, format
tables, and divide long books into braille volumes.
[APH's 2014 research report][report] · [Public beta announcement][beta]

APH announced the official launch at its annual meeting on October 12,
2017, and made BrailleBlaster available as a free download. Textbooks
drove its development, and teachers and families could also use it to
produce worksheets, tests, and other reading materials. In June 2021,
APH introduced BrailleBlaster 2, a near-complete rewrite that added direct
six-key braille entry and indicators for editing braille page numbers.
[2017 launch announcement][launch] · [APH product overview][product] ·
[Version 2 announcement][v2]

On March 26, 2025, APH released BrailleBlaster 3.0 under the GNU General
Public License version 3 and made the source code available on GitHub.
The release also introduced automatic updates for users of the installer
and packages for 64-bit ARM Linux systems. Later 3.x releases continued
to improve document importing, braille translation, and math output. In
December 2025, the DAISY Consortium reported that BrailleBlaster had
committed to supporting eBraille. That work carries forward an aim from
Boyer's original announcement: making braille documents that readers can
use on a refreshable display as well as on embossed paper.
[Version 3.0 announcement][v3] · [GitHub releases][releases] ·
[DAISY eBraille project update][ebraille]

[announcement]: https://nfbnet.org/pipermail/nfb-science_nfbnet.org/2010-July/000343.html
[boyer]: https://obamawhitehouse.archives.gov/blog/2012/05/07/eliminating-barriers-entry-increasing-braille-used-stem
[report]: https://sites.aph.org/research-report-2014/ar2014.html
[beta]: https://sites.aph.org/news/november-2016/
[launch]: https://www.aph.org/app/uploads/2019/02/October-25-2017-BrailleBlaster.pdf
[product]: https://www.aph.org/product/brailleblaster/
[v2]: https://www.aph.org/blog/even-easier-braille-introducing-brailleblaster-v2/
[v3]: https://www.braillists.org/uncategorised/announcing-brailleblaster-3-0-0/

[ebraille]: https://daisy.org/news-events/articles/ebraille-project-update/
[releases]: https://github.com/aphtech/brailleblaster/releases

## Obtaining BrailleBlaster

### Installers

Download the latest release for Windows, macOS, or Linux from either of
these places:

- [BrailleBlaster home page](https://brailleblaster.org)
- [GitHub releases][releases]

### Development builds

If you want the newest changes and accept the risks of untested software,
try the [continuous release](https://github.com/aphtech/brailleblaster/releases/continuous).
See [Running a development build](#running-a-development-build) below.

### Source code

The source code is on GitHub at
[aphtech/brailleblaster](https://github.com/aphtech/brailleblaster).
Clone it with:

```sh
git clone https://github.com/aphtech/brailleblaster.git
```

## Building BrailleBlaster

BrailleBlaster uses the maven build system and requires a Java Development Kit of Java25 or higher to be installed. You do not need to have maven installed as BrailleBlaster includes some maven wrapper scripts which will be able to download the required version of maven. To build BrailleBlaster run the following command at the root of the source tree:
```command line
mvnw package
```
Once the build finishes you will find the application in brailleblaster-app/target/dist.

## Running tests

By default the build runs only headless tests. Tests that open BrailleBlaster windows are marked `@GuiTest` and are put in the `gui` group, which runs only when you ask for it.

| Want | Command |
|---|---|
| Headless only (default) | `mvnw test` |
| Everything | `mvnw test -Dgui` |
| GUI only | `mvnw test -pl brailleblaster-core -Pgui-only` |
| One GUI class | `mvnw test -pl brailleblaster-core -Dgui "-Dtest=EditAfterPageBreakTest"` |
| Include broken groups | add `-Dbb.excludedTests=none` |

GUI tests take over the desktop. On Windows, keep the desktop unlocked and don't touch the mouse or keyboard while they run.

New test classes that create a `BBTestRunner`, `SWTBot`, or an SWT `Shell` must be marked `@GuiTest`. `GuiGroupTaggingTest` fails the headless build if one isn't.

## Running a development build

To run a development build of BrailleBlaster, either one you built yourself or from the continuous release, you will need Java25 or higher installed. On Windows or Linux issue the following command from the root of your build:
```command line
java -jar brailleblaster.jar
```
Mac users will need a different command which is:
```bash
java -XstartOnFirstThread -jar brailleblaster.jar
```

## BrailleBlaster Contributors

BrailleBlaster has been developed with support from the following organizations:

- **American Printing House for the Blind (APH)**
- **ViewPlus Technologies, Inc.**
- **Abilitiessoft, Inc.**

### Contributions by commit

Contributors are listed by their number of non-merge commits across both BrailleBlaster repositories, 18,037 in total:
This may not be the best way to track contributions.  If someone has a better method feel free to submit a PullRequest.



- **The original repository (`bb-utd`):** 16,725 commits starting in November 2010. All but 3 were made before the split.
- **The open-source repository on GitHub (`aphtech/brailleblaster`):** 1,312 commits on `main` since the split on January 16, 2025.

Commits from pull requests and other branches merged into `main` are counted for the people who wrote them. Only the merge commits themselves are left out, because they record who merged a branch rather than who did the work. The two histories do not overlap, so no commit is counted twice. Commits made under different names or email addresses by the same person are combined. People who worked for more than one organization have one combined total, with each organization listed.

Commit count is only a rough measure. It does not show how large or difficult a change was, and it leaves out design, testing, documentation, and work done outside these repositories.

| Contributor | Affiliation | Commits |
|---|---|---:|
| Michael Whapples | ViewPlus Technologies, Inc.; APH | 4,812 |
| Leon Blakey | APH | 3,002 |
| Rebecca Luttmer | APH | 2,819 |
| Corey Knapp | APH | 1,869 |
| Brandon Roller | APH | 1,547 |
| Rezylle Milallos | APH | 1,314 |
| Keith Creasy | APH | 872 |
| John J. Boyer | Abilitiessoft, Inc. | 319 |
| Will Tribbey | APH | 291 |
| Chuck Myers | APH | 262 |
| Leyvis Valdes | APH | 196 |
| Ken Perry | APH | 195 |
| Matt Stewart | APH | 159 |
| Mike Gray | APH | 120 |
| N. Rodriguez | | 99 |
| Sahel Mastoureshgh | | 68 |
| François Ouellette | | 67 |
| John Hedges | APH | 17 |
| Paul Faust | APH | 3 |
| Bobby | | 3 |
| Willo Free | APH | 2 |
| Jage9 | | 1 |

#### Last commit counted

| Repository | Branch | Last commit counted | Date |
|---|---|---|---|
| `bb-utd` (original) | `main` | `38b42555f3d9fa36887e6f5697d81fb889a1af1f` | 2026-10-27 |
| `aphtech/brailleblaster` (GitHub) | `main` | `79928e6201c634af90a880891d8863260ef6bf4b` | 2026-11-24 |

#### Updating these counts

To add newer work, count the commits made after the last commit counted in each repository, then add them to the table:

```sh
git log <last-commit>..main --no-merges --format='%aN <%aE>' | sort | uniq -c | sort -rn
```

Combine commits that one person made under different names or email addresses, and add anyone new to the table. Then replace the commit hashes and dates above with the newest commits counted.

### Other contributors

These people are credited in the project's contributor files but have no commits under their own names in either repository's history. Their work may come from before these repositories or may have reached them through other people.

- John Gardner (ViewPlus Technologies, Inc.)
- Hanxiao Fu
- Sina Bahram
- Vic Beckley

## QA or Tester

I also want to thank all the people who are not mentioned here for all their testing.  If you have a QA name that should be placed here let me know.

- Mark Rohret 
- Joe Hodge



