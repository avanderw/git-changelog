# net.avdw.git-changelog
![top-language](https://img.shields.io/github/languages/top/avanderw/git-changelog)
![license](https://img.shields.io/github/license/avanderw/git-changelog)

_Git changelog transformer_

## Showcase

## Getting started

```shell script
$ git clone https://github.com/avanderw/git-changelog.git
$ cd git-changelog/
$ mvn verify
$ java -jar ./target/git-changelog-jar-with-dependencies.jar --help
Usage: git-changelog [-hV] [COMMAND]
Git changelog transformer
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
Commands:
```

## Usage

## Configuration

### Logging
_./tinylog.properties_
```properties
writer        = file
writer.file   = git-changelog.log
writer.level  = info
writer.format = [{level}] at .({class-name}.java:{line}) {message}
```
Further documentation can be found at [tinylog.org](https://tinylog.org/v2/configuration/)

## Support

### Installing supporting software
> It is recommended to make use of a package manager to simplify the setup of your environment

- Java 11
- Maven 3.3.9

#### Chocolatey (https://chocolatey.org/install)
```cmd
$ choco install ojdkbuild
$ choco install maven 
```

## Changelog
![last-commit](https://img.shields.io/github/last-commit/avanderw/git-changelog)
 
All notable changes to this project will be documented in [CHANGELOG](CHANGELOG.md). 
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) 
and adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Roadmap
Refer to the file [.todo/todo.txt](.todo/todo.txt) for a list of potential future work.
Here is a [complete primer](https://github.com/todotxt/todo.txt) on the whys and hows of todo.txt.

## Contributing
![commit-activity](https://img.shields.io/github/commit-activity/y/avanderw/git-changelog)
 
We love contributions! Please read [CONTRIBUTING](CONTRIBUTING.md) for details on how to contribute.

## License 
This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details
