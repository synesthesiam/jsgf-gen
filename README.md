JSGF Sentence Generator
============================

Tool for generating sentences from [JSGF Grammars](https://www.w3.org/TR/jsgf/).

All credit for tag support goes to [candrews](https://github.com/candrews). See the [commander](https://github.com/candrews/commander) for a complete source listing.

Building
--------

Run `make` in the root or `gradle installDist`.

Generating Sentences
------------------------

The `jsgf-gen` tool takes a grammar file as input (`--grammar`) and outputs sentences.
The input grammar may `import` or reference other grammars in the same directory.

Here's an example grammar (`etc/basic_command.gram`):

    #JSGF V1.0;
    grammar basic_command;

    public <basicCmd> = <startPolite> <command> <endPolite>;

    <command> = <action> <object>;
    <action> = (/10/ open | /2/ close | /1/ delete | /1/ move) {action};
    <object> = [the | a] (window | file | menu) {object};

    <startPolite> = (please | kindly | could you | oh mightly computer) *;
    <endPolite> = [please | thanks | thank you**;

### Random Sentences

Generate 10 random sentences:

    $ jsgf-gen --grammar etc/basic_command.gram --count 10

    kindly close window
    oh mightly computer open file
    please open window
    kindly open a window
    oh mightly computer open window
    please open a window
    please open the window
    please close file
    could you open window

You can also set the random seed with `--seed`

### All Sentences

Enumerate **all** sentences in your grammar (don't do this if your grammar is infinite!):

    $ jsgf-gen --grammar etc/basic_command.gram --exhaustive

    please open the window
    please open the file
    please open a window
    please open a file
    please open window
    please open file
    please close the window
    please close the file
    please close a window
    please close a file
    please close window
    please close file
    kindly open the window
    kindly open the file
    kindly open a window
    kindly open a file
    kindly open window
    kindly open file
    kindly close the window
    kindly close the file
    kindly close a window
    kindly close a file
    kindly close window
    kindly close file
    could you open the window
    could you open the file
    could you open a window
    could you open a file
    could you open window
    could you open file
    could you close the window
    could you close the file
    could you close a window
    could you close a file
    could you close window
    could you close file
    oh mightly computer open the window
    oh mightly computer open the file
    oh mightly computer open a window
    oh mightly computer open a file
    oh mightly computer open window
    oh mightly computer open file
    oh mightly computer close the window
    oh mightly computer close the file
    oh mightly computer close a window
    oh mightly computer close a file
    oh mightly computer close window
    oh mightly computer close file

### Tagged Sentences

Add `--tags` to have the sentences contain inline tags in Markdown style:

    $ jsgf-gen --grammar etc/basic_command.gram --count 10  --tags

    kindly [open](action) [window](object)
    could you [close](action) the [window](object)
    please [open](action) [file](object)
    could you [close](action) [file](object)
    oh mightly computer [open](action) a [file](object)
    please [open](action) [window](object)
    could you [open](action) [window](object)
    kindly [open](action) a [file](object)

Replace `--tags` with `--classes` to print upper-cased class names instead:

    $ jsgf-gen --grammar etc/basic_command.gram --count 10  --classes

    please ACTION OBJECT
    could you ACTION the OBJECT
    could you ACTION OBJECT
    oh mightly computer ACTION the OBJECT
    oh mightly computer ACTION a OBJECT
    kindly ACTION OBJECT
    oh mightly computer ACTION OBJECT


Tokens
-------

You can get a unique listing of all tokens in the grammar with:

    $ jsgf-gen --grammar etc/basic_command.gram --tokens

    a
    could
    please
    the
    kindly
    computer
    file
    mightly
    oh
    window
    close
    open
    you


Replacing Alternatives
---------------------------

Create a new grammar with one or more "alternative sets" rules replaced:


    $ jsgf-gen --grammar etc/basic_command.gram --replace <(echo '{ "action": ["move", "delete" ] }')

    #JSGF V1.0;
    grammar basic_command;

    <startPolite> = ( please | kindly | could you | oh mightly computer );
    <action> = move | delete;
    public <basicCmd> = <startPolite> <command>;
    <command> = <action> <object>;
    <object> = [the | a] (window | file) {object};
    
This replaced the `<action>` right-hand side with `move | delete` instead of `open | close`.
