<p align="center"><img width="256" src="https://github.com/Phylogeny/Discernment/blob/1.16.1/src/main/resources/logo.png?raw=true" /></p>
<h1 align="center">Discernment</h1>
This mod adds a discernment enchantment and mob effect that prevents a living entity (including a player) from damaging the following types of other living entities (with configs to disable any/all of them):
<ol>
    <li>Entities that do not have a custom name (such as given via a name tag) that matches a config-defined regular expression blacklist (default behavior matches any name that contains the word "unprotected" -- case-independent) AND:</li>
    <li>Entities that have a custom name that matches a config-defined regular expression whitelist (default behavior matches any name that contains the word "protected" -- case-independent) OR:</li>
    <li>Ownable entities that are owned by the entity with discernment OR:</li>
    <li>Peaceful non-player entities OR:</li>
    <li>Players</li>
</ol>
This protection only applies while the following conditions are met:
<ol>
    <li>The attacking entity either:
        <ol>
            <li>Has the discernment mob effect</li>
            <li>Is wearing an item as armor, or holding an item in their main or offhand, that has the discernment enchantment on it</li>
        </ol>
    </li>
    <li>The attacking entity is not sneaking</li>
</ol>