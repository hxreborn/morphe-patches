.class public Lcom/myscript/nebo/clipboard/ClipboardContentProvider;
.super Ljava/lang/Object;

.method public query(Landroid/net/Uri;[Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;)Landroid/database/Cursor;
    .registers 10

    iget-object p3, p0, Lcom/myscript/nebo/clipboard/ClipboardContentProvider;->mUriMatcher:Landroid/content/UriMatcher;

    if-nez p3, :cond_a

    invoke-static {}, Lcom/myscript/nebo/clipboard/ClipboardContentProvider;->buildUriMatcher()Landroid/content/UriMatcher;

    move-result-object p3

    iput-object p3, p0, Lcom/myscript/nebo/clipboard/ClipboardContentProvider;->mUriMatcher:Landroid/content/UriMatcher;

    :cond_a
    new-instance p3, Landroid/net/Uri$Builder;

    invoke-direct {p3}, Landroid/net/Uri$Builder;-><init>()V

    invoke-virtual {p1}, Landroid/net/Uri;->getScheme()Ljava/lang/String;

    move-result-object p4

    invoke-virtual {p3, p4}, Landroid/net/Uri$Builder;->scheme(Ljava/lang/String;)Landroid/net/Uri$Builder;

    move-result-object p3

    invoke-virtual {p1}, Landroid/net/Uri;->getAuthority()Ljava/lang/String;

    move-result-object p4

    invoke-virtual {p3, p4}, Landroid/net/Uri$Builder;->authority(Ljava/lang/String;)Landroid/net/Uri$Builder;

    move-result-object p3

    invoke-virtual {p1}, Landroid/net/Uri;->getPathSegments()Ljava/util/List;

    move-result-object p4

    invoke-interface {p4}, Ljava/util/List;->size()I

    move-result p4

    const/4 p5, 0x0

    if-lez p4, :cond_35

    invoke-virtual {p1}, Landroid/net/Uri;->getPathSegments()Ljava/util/List;

    move-result-object p4

    invoke-interface {p4, p5}, Ljava/util/List;->get(I)Ljava/lang/Object;

    move-result-object p4

    check-cast p4, Ljava/lang/String;

    goto :goto_37

    :cond_35
    const-string p4, ""

    :goto_37
    invoke-virtual {p3, p4}, Landroid/net/Uri$Builder;->appendPath(Ljava/lang/String;)Landroid/net/Uri$Builder;

    move-result-object p3

    invoke-virtual {p3}, Landroid/net/Uri$Builder;->build()Landroid/net/Uri;

    move-result-object p3

    iget-object p4, p0, Lcom/myscript/nebo/clipboard/ClipboardContentProvider;->mUriMatcher:Landroid/content/UriMatcher;

    invoke-virtual {p4, p3}, Landroid/content/UriMatcher;->match(Landroid/net/Uri;)I

    move-result p3

    const/16 p4, 0x64

    if-eq p3, p4, :cond_4b

    const/4 p0, 0x0

    return-object p0

    :cond_4b
    iget-object p0, p0, Lcom/myscript/nebo/clipboard/ClipboardContentProvider;->mStrategy:Lcom/myscript/nebo/clipboard/ClipboardContentProvider$PathStrategy;

    invoke-interface {p0, p1}, Lcom/myscript/nebo/clipboard/ClipboardContentProvider$PathStrategy;->getFileForUri(Landroid/net/Uri;)Ljava/io/File;

    move-result-object p0

    if-nez p2, :cond_55

    sget-object p2, Lcom/myscript/nebo/clipboard/ClipboardContentContract$FileEntry;->FILE_COLUMNS:[Ljava/lang/String;

    :cond_55
    array-length p1, p2

    new-array p1, p1, [Ljava/lang/String;

    array-length p3, p2

    new-array p3, p3, [Ljava/lang/Object;

    array-length p4, p2

    move v0, p5

    :goto_5d
    if-ge p5, p4, :cond_a2

    aget-object v1, p2, p5

    const-string v2, "_display_name"

    invoke-virtual {v2, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-eqz v3, :cond_75

    aput-object v2, p1, v0

    add-int/lit8 v1, v0, 0x1

    invoke-virtual {p0}, Ljava/io/File;->getName()Ljava/lang/String;

    move-result-object v2

    aput-object v2, p3, v0

    :goto_73
    move v0, v1

    goto :goto_9f

    :cond_75
    const-string v2, "_size"

    invoke-virtual {v2, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-eqz v3, :cond_8c

    aput-object v2, p1, v0

    add-int/lit8 v1, v0, 0x1

    invoke-virtual {p0}, Ljava/io/File;->length()J

    move-result-wide v2

    invoke-static {v2, v3}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;

    move-result-object v2

    aput-object v2, p3, v0

    goto :goto_73

    :cond_8c
    const-string v2, "_path"

    invoke-virtual {v2, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_9f

    aput-object v2, p1, v0

    add-int/lit8 v1, v0, 0x1

    invoke-virtual {p0}, Ljava/io/File;->getPath()Ljava/lang/String;

    move-result-object v2

    aput-object v2, p3, v0

    goto :goto_73

    :cond_9f
    :goto_9f
    add-int/lit8 p5, p5, 0x1

    goto :goto_5d

    :cond_a2
    invoke-static {p1, v0}, Lcom/myscript/nebo/clipboard/ClipboardContentProvider;->copyOf([Ljava/lang/String;I)[Ljava/lang/String;

    move-result-object p0

    invoke-static {p3, v0}, Lcom/myscript/nebo/clipboard/ClipboardContentProvider;->copyOf([Ljava/lang/Object;I)[Ljava/lang/Object;

    move-result-object p1

    new-instance p2, Landroid/database/MatrixCursor;

    const/4 p3, 0x1

    invoke-direct {p2, p0, p3}, Landroid/database/MatrixCursor;-><init>([Ljava/lang/String;I)V

    invoke-virtual {p2, p1}, Landroid/database/MatrixCursor;->addRow([Ljava/lang/Object;)V

    return-object p2
.end method
