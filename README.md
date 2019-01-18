## What does this do?
This project aims to provide a fast and platform-independent pointer searcher to aid cheat code creation. It was developed and tested to function with `Nintendo Wii U` memory dumps but should be compatible with other consoles such as the `Nintendo 3DS` as well due to configurable byte order and starting address.

## Where do I download a compiled version?
[Here](https://github.com/BullyWiiPlaza/Universal-Pointer-Searcher/blob/master/Universal-Pointer-Searcher.jar?raw=true).

## How do I execute the JAR file?
Since this software is Java-based, you need to have [`JRE 11`](https://www.oracle.com/technetwork/java/javase/downloads/jdk11-downloads-5066655.html) or higher installed on your machine. On `Windows`, a simple double-click on the `JAR` file should suffice. On `Unix`, run the command `java -jar "Universal-Pointer-Search.jar"`.

## I started the application. How do I use it to find pointers?
A tutorial can be found [here](https://www.youtube.com/watch?v=KUEXUZuO0qc).

## Why does the program not start or stop right after I start a search?
You're most likely running out of memory.
You need to have at least as much RAM available as the full size of one of the RAM dumps you're using.
If your machine does not have enough RAM, consider using smaller memory dumps or try [Mr. Mysterio's `Pointer Searcher`](https://gbatemp.net/threads/pointer-searcher.397783/).
Also, if you run into freezing/lag issues before hitting your RAM limit, [passing the `-Xmx` VM argument](https://stackoverflow.com/questions/5374455/what-does-java-option-xmx-stand-for) followed by your total available PC RAM (e.g. `16g` for `16GB`) can be helpful since it disables the general memory limit imposed by default by Java.

Also, [I recommend using a 64-bit JRE](http://stackoverflow.com/a/1434901/3764804) because it supports more than `4GB` of RAM being assigned to the process. Some searches will not finish if you have less memory than that.

## Pointer searching is still too slow. How to speed it up?
* You can cut off bytes from your memory dumps at the beginning and at the end but make sure to keep all potential pointer base addresses and target addresses within. Make sure to increase the memory dump starting address accordingly if you cut off bytes at the beginning
* Using a higher pointer depth significantly slows down performance so do not use it if you want the search to complete faster
* Reduce the maximum offset since it makes search intervals bigger and thus can increas searching time by a lot
* Deselect allowing negative offsets since it basically doubles the maximum offset

## Which features does this program offer?
* Negative pointer offsets
* Selectable byte order
* Unlimited pointer depth searches
* Customizable starting address
* Customizable maximum pointer offset
* Unlimited memory dumps
* Adding entire folders
* Generating and working with pointer maps
* Restricted base offset range to reduce pointer results
* Sorting pointers by different criteria
* Configurable maximum memory chunk size for low RAM users

## What are pointer maps and what do they do?
Pointer maps are files containing base offsets and their values from the memory dump. Only potential pointers are listed. The pointer map file can also be used to perform pointer searches. Once you created a pointer map, you can theoretically delete the memory dump to save storage space on your PC and loading the pointer map by the pointer searcher may be faster than entire memory dumps. These are the advantages of using pointer maps.

## How fast is this pointer searcher? Isn't Java suboptimal for tasks like this?
No, the application is very fast. It uses optimized algorithms which matters more than a simple language choice. My short testing has shown that Java was only about 30% slower than optimized C/C++ but it's a lot faster to develop. This application is also significantly faster and more configurable than Mr. Mysterio's `Pointer Searcher` but also has higher memory usage.

## Who gets credit for the creation of this application?
[BullyWiiPlaza](https://www.youtube.com/channel/UC8aTT6he9SCxO6gygCYqCUA)
