# Csoport 3 szerver használati útmutató
csak akkor működik ha ez a kód fut éppen btw

subscribe saját topicra (név, nem muszáj egyenlő legyen a kliens id-vel, de legyen egyedi)
subscribe Armageddon
Login topicra send név

-> saját topicról kell kapni egy Parked-et

ha státusz Parked, még nem vettél fel csomagot, nincs előtted másik kocsi Starter topicra küldd a neved (amíg fel nem veszed addig mindenki a rajtvonalon Halt-on lesz)
-> saját topicra kap egy Started-et

ha armageddonra megy üzenet akkor minden kocsi Armageddon státuszt kap -> letelik egy kis idő majd visszakapja a régi státuszát

doboz sikeres felvételekor Loadedre kell a következőt küldeni: név,csomagid -> minden Haltot Parked-ra rak, téged Work In Progress-re

egyelőre ennyi