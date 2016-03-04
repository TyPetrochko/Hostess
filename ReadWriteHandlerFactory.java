public class ReadWriteHandlerFactory implements
        ISocketReadWriteHandlerFactory {
    public IReadWriteHandler createHandler() {
        return new ReadWriteHandler();
    }
}
