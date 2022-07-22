# Controlling Timed Automata for opacity

X is an open source software tool to perform the exhibition of a controller for opacity, ie a set of controllable actions, such that the system is fully timed-opaque [ALMS23](https://doi.org/10.1145/3502851). It iteratively constructs strategies and check full timed opacity.

## Using the tool
### Requirements
X requires a functional installation of:
- [IMITATOR](https://imitator.fr)
- [PolyOp](https://github.com/etienneandre/PolyOp)

### Call syntax
A basic call to the tool can be performed with:
```java controlling -file [path]```

The options that can be used while calling X are the followings:
```
	- Required:
 * -file [path]		 Path to the imi file [REQUIRED]

	- Optional:
 * -actions [actions]	List of controllable actions, separated with a comma (if not set, use all actions)
 * -efficient	 		Exclude non efficient strategies in opacity (otherwise, include them)
 * -lf [name]		 	Name of the final location (default: qf)
 * -lpriv [name]	 	Name of the private location (default: qpriv)
 * -find [find]		 	Description of the set to find ('min', 'max', 'all') (default: all)
 * -witness		 		Stop as soon as a full timed-opaque strategy is found  (default: false)

- Binary paths (optional):
 * -imitator [path]	Path to the binary file of IMITATOR  (default: imitator)
 * -polyop [path]	 	Path to the binary file of PolyOp  (default: polyop)
```