-dontobfuscate
-keep class kotlin.uuid.Uuid {
    public static final kotlin.uuid.Uuid$Companion Companion;
    public java.lang.String toString();
    private final java.lang.Object writeReplace();
    private final void readObject(java.io.ObjectInputStream);
}
-keep class kotlin.uuid.Uuid$Companion {
    public kotlin.uuid.Uuid parse(java.lang.String);
}
-keep class kotlin.uuid.UuidSerialized {
    private static final long serialVersionUID;
    public <init>();
    public void writeExternal(java.io.ObjectOutput);
    public void readExternal(java.io.ObjectInput);
    private final java.lang.Object readResolve();
}
