package foo.starred.odinclient.api.storage

import foo.starred.kontext.api.storage.impl.generic.impl.GenericJsonStore

class JsonStore(path: String) : GenericJsonStore("odinClient", path)
