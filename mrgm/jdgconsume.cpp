#include <qpid/messaging/Connection.h>
#include <qpid/messaging/Session.h>
#include <qpid/messaging/Sender.h>
#include <qpid/messaging/Receiver.h>
#include <qpid/messaging/Message.h>
#include <qpid/messaging/Duration.h>
#include <qpid/sys/Thread.h>
#include <qpid/sys/Runnable.h>
#include <qpid/sys/Time.h>
#include <iostream>
#include <boost/program_options.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/ptr_container/ptr_vector.hpp>
#include <jni.h>

using namespace std;
using namespace qpid::messaging;
using namespace qpid::sys;
using boost::ptr_vector;
using boost::lexical_cast;
namespace po = boost::program_options;

#define PATH_SEPARATOR ':'

bool ssl = false;
const int numRandomMessages = 1000;

class Options {
public:
  string senderBroker;
  string senderAddress;
  string receiverBroker;
  string receiverAddress;
  int messageCount;
  int capacity;
  int messageSize;
  int ack;
  bool sendOnly;
  bool receiveOnly;
  bool noThreads;
  int numSenders;
  int numReceivers;
  string sslCertDb;
  string sslCertPasswordFile;
  string sslCertName;
  int headers;
  bool tcpNoDelay;
  int numRandomMessages;
  string jdgProps;
  string queueName;

  po::options_description desc;
 
  explicit Options() : desc("Allowed options") {
    senderBroker = "localhost";
    receiverBroker = "localhost";
    messageCount = 100000;
    capacity = 1000;
    messageSize = 300;
    ack = 1000;
    sendOnly = false;
    receiveOnly = true;
    noThreads = false;
    numSenders = 1;
    numReceivers = 1;
    headers = 0;
    tcpNoDelay = true;
    jdgProps = "smartdata-client.properties";
    queueName = "intakequeue";
    desc.add_options()
      ("help", "this help message")
      ("sender-broker,s", po::value<string>(&senderBroker), "the broker the sender should use")
      ("sender-address,d", po::value<string>(&senderAddress), "the exchange the sender should send to")
      ("receiver-broker,r", po::value<string>(&receiverBroker), "the broker the receiver should use")
      ("receiver-address,c", po::value<string>(&receiverAddress), "the queue to read from")
      ("messages,m", po::value<int>(&messageCount), "number of messages to send")
      ("buffer,b", po::value<int>(&capacity), "buffer capacity")
      ("message-size,S", po::value<int>(&messageSize), "message size, in bytes")
      ("ack,a", po::value<int>(&ack), "number of acknowledgements to batch in a transaction")
      ("send-only", po::value<bool>(&sendOnly), "only run the sender thread")
      ("receive-only", po::value<bool>(&receiveOnly), "only run the receiver thread")
      ("no-threads", po::value<bool>(&noThreads), "runs the sender first, then the receiver, in sequence without threading")
      ("num-senders", po::value<int>(&numSenders), "number of sender threads")
      ("num-receivers", po::value<int>(&numReceivers), "number of receiver threads")
      ("ssl-cert-db", po::value<string>(&sslCertDb), "location of certificate database")
      ("ssl-cert-password-file", po::value<string>(&sslCertPasswordFile), "location of certificate database password file")
      ("ssl-cert-name", po::value<string>(&sslCertName), "name of certificate to use")
      ("headers", po::value<int>(&headers), "# of headers")
      ("tcp-nodelay", po::value<bool>(&tcpNoDelay), "tcp nodelay")
      ("jdg-props,j", po::value<string>(&jdgProps), "data grid client properties file name")
      ("queue-name,q", po::value<string>(&queueName), "queue name to receive from; don't use if multithreaded")
    ;
  }

  void parse(int argc, char** argv) {
    po::variables_map vm;
    po::store(po::parse_command_line(argc, argv, desc), vm);
    po::notify(vm);

    if (vm.count("help")) {
      std::cout << desc << std::endl;
      exit(0);
    }
  }
};

class Base : public Runnable {
public:
  explicit Base(int index, Options& options, Connection& connection);
  virtual ~Base();
  string stats();

  int index;
  Options options;
  Connection connection;
  Message messages[numRandomMessages];
  Session session;
  double total;

  AbsTime start, end;
};

Base::Base(int index, Options& options, Connection& connection) : index(index), options(options), connection(connection), total(0) {
  srand(time(NULL));
  for(int j=0; j < numRandomMessages; j++) {
    string content = "{\"key\":123456789,\"time\":\"2012-11-10 00:47:01\",\"type\":\"c\",\"app\":\"any\",\"orig\":\"ge-1/0/0.120\"";
    content += ",\"origIp\":\"192.168." + boost::lexical_cast<string>(rand() % 256) + "." + boost::lexical_cast<string>(rand() % 256) + "\"";
    //content += ",\"origPort\":\"" + boost::lexical_cast<string>(rand() % 10 + 10000) + "\"";
    content += ",\"origPort\":" + boost::lexical_cast<string>(rand() % 10 + 10000) + "";
    content += ",\"destIp\":\"192.168." + boost::lexical_cast<string>(rand() % 256) + "." + boost::lexical_cast<string>(rand() % 256) + "\"";
    //content += ",\"destPort\":\"" + boost::lexical_cast<string>(rand() % 10 + 10100) + "\"";
    content += ",\"destPort\":" + boost::lexical_cast<string>(rand() % 10 + 10100) + "";
    content += ",\"transIp\":\"192.168." + boost::lexical_cast<string>(rand() % 256) + "." + boost::lexical_cast<string>(rand() % 256) + "\"";
    //content += ",\"transPort\":\"" + boost::lexical_cast<string>(rand() % 10 + 10200) + "\"}";
    content += ",\"transPort\":" + boost::lexical_cast<string>(rand() % 10 + 10200) + "}";

    //cout << "Message=" << content <<endl;

    Message message = Message(content);
    for (int i = 0; i < options.headers; i++) {
      message.getProperties()["header" + lexical_cast<string>(i)] = "value" + lexical_cast<string>(i);
    }

    messages[j] = message;
  }

  if(options.tcpNoDelay) {
    connection.setOption("tcp-nodelay",1);
  }

  if(ssl) {
    cout << "Enabling ssl" << endl;
    connection.setOption("transport", "ssl");
  }

  connection.open();

  session = connection.createSession();
}

Base::~Base() {
  session.close();
  connection.close();
}

string Base::stats() {
  stringstream ss;
  qpid::sys::Duration duration = qpid::sys::Duration(start,end);
  double seconds = double(duration)/TIME_SEC;
  total = options.messageCount / seconds;

  ss << seconds << " seconds, "
     << total << " messages/sec";
  return ss.str();
}

class Producer : public Base {
public:
  explicit Producer(int index, Options& options, Connection& connection) : Base(index, options, connection) {}
  void run();
};

void Producer::run() {
  //string queueName = "my-queue" + lexical_cast<string>(index);
  string queueName = options.queueName;
  string address = queueName;
  if (options.senderAddress != "") {
    address = options.senderAddress;
  }
  Sender sender = session.createSender(address);
  sender.setCapacity(options.capacity);

  cout << "Sending messages" << endl;
  start = now();
  bool done = false;
  int i = 0;
  while(!done) {
    //Message message = makeMessage(options);
    if (options.messageCount > 0 && i >= options.messageCount) {
      done = true;
      break;
    }
    sender.send(messages[rand() % numRandomMessages]);
    i++;

    if(i % 100000 == 0) {
      cout << "Produced " << i << " messages" << endl;
    }
  }
  end = now();
  sender.close();
}

class Consumer : public Base {
public:
  explicit Consumer(int index, Options& options, Connection& connection) : Base(index, options, connection) {}
  void run();
  void writeToISPN(std::string str);
};

void Consumer::run() {
  //string queueName = "my-queue" + lexical_cast<string>(index) + "; {create:always, delete:never}";
  string queueName = options.queueName;
  string address = queueName;
  if (options.receiverAddress != "") {
    address = options.receiverAddress;
  }
  Receiver receiver = session.createReceiver(address);
  session.sync(true);
  receiver.setCapacity(options.capacity);
  Message message;
  cout << "Receiving messages" << endl;
  bool done = false;
  int i = 0;

  //JNI init
  cout << "Initializing JVM and the data grid." << endl;

  JavaVMOption jvmoptions[3];
  JNIEnv *env;
  JavaVM *jvm;
  JavaVMInitArgs vm_args;
  long status;
  jclass cls;
  jmethodID mid;

  string classpath = "-Djava.class.path=target/classes/";
  classpath += ":.";
  classpath += ":target/dependency/antlr-2.7.7.jar";
  classpath += ":target/dependency/avro-1.5.1.jar";
  classpath += ":target/dependency/commons-codec-1.4.jar";
  classpath += ":target/dependency/commons-collections-3.2.1.jar";
  classpath += ":target/dependency/commons-io-2.1.jar";
  classpath += ":target/dependency/commons-lang-2.6.jar";
  classpath += ":target/dependency/smartdata-ejb-0.0.1-SNAPSHOT-client.jar";
  classpath += ":target/dependency/dom4j-1.6.1.jar";
  classpath += ":target/dependency/guava-11.0.2.jar";
  classpath += ":target/dependency/hibernate-commons-annotations-4.0.1.Final.jar";
  classpath += ":target/dependency/hibernate-core-4.0.1.Final.jar";
  classpath += ":target/dependency/hibernate-jpa-2.0-api-1.0.1.Final.jar";
  classpath += ":target/dependency/hibernate-search-4.1.0.Alpha1.jar";
  classpath += ":target/dependency/hibernate-search-analyzers-4.1.0.Alpha1.jar";
  classpath += ":target/dependency/hibernate-search-engine-4.0.0.CR2.jar";
  classpath += ":target/dependency/hibernate-search-infinispan-4.1.0.Alpha1.jar";
  classpath += ":target/dependency/hibernate-search-orm-4.1.0.Alpha1.jar";
  classpath += ":target/dependency/infinispan-core-5.1.2.FINAL.jar";
  classpath += ":target/dependency/infinispan-lucene-directory-5.1.0.CR2.jar";
  classpath += ":target/dependency/infinispan-query-5.1.0.CR2.jar";
  classpath += ":target/dependency/jackson-core-asl-1.9.2.jar";
  classpath += ":target/dependency/jackson-mapper-asl-1.9.2.jar";
  classpath += ":target/dependency/javassist-3.15.0-GA.jar";
  classpath += ":target/dependency/jboss-ejb-api_3.1_spec-1.0.2.Final.jar";
  classpath += ":target/dependency/jboss-ejb-client-1.0.5.Final.jar";
  classpath += ":target/dependency/jboss-logging-3.1.0.GA.jar";
  classpath += ":target/dependency/jboss-marshalling-1.3.11.GA.jar";
  classpath += ":target/dependency/jboss-marshalling-river-1.3.11.GA.jar";
  classpath += ":target/dependency/jboss-remoting-3.2.3.GA.jar";
  classpath += ":target/dependency/jboss-sasl-1.0.0.Final.jar";
  classpath += ":target/dependency/jboss-transaction-api_1.1_spec-1.0.1.Final.jar";
  classpath += ":target/dependency/lucene-analyzers-3.5.0.jar";
  classpath += ":target/dependency/lucene-core-3.4.0.jar";
  classpath += ":target/dependency/lucene-grouping-3.5.0.jar";
  classpath += ":target/dependency/lucene-highlighter-3.5.0.jar";
  classpath += ":target/dependency/lucene-memory-3.5.0.jar";
  classpath += ":target/dependency/lucene-misc-3.5.0.jar";
  classpath += ":target/dependency/lucene-smartcn-3.5.0.jar";
  classpath += ":target/dependency/lucene-spatial-3.5.0.jar";
  classpath += ":target/dependency/lucene-spellchecker-3.5.0.jar";
  classpath += ":target/dependency/lucene-stempel-3.5.0.jar";
  classpath += ":target/dependency/paranamer-2.3.jar";
  classpath += ":target/dependency/rhq-pluginAnnotations-3.0.4.jar";
  classpath += ":target/dependency/slf4j-api-1.6.1.jar";
  classpath += ":target/dependency/snappy-java-1.0.1-rc3.jar";
  classpath += ":target/dependency/solr-analysis-extras-3.5.0.jar";
  classpath += ":target/dependency/solr-commons-csv-3.5.0.jar";
  classpath += ":target/dependency/solr-core-3.5.0.jar";
  classpath += ":target/dependency/solr-solrj-3.5.0.jar";
  classpath += ":target/dependency/xnio-api-3.0.3.GA.jar";
  classpath += ":target/dependency/xnio-nio-3.0.3.GA.jar";
  classpath += ":target/dependency/hornetq-core-client-2.2.13.Final.jar";
  classpath += ":target/dependency/hornetq-jms-client-2.2.13.Final.jar";
  classpath += ":target/dependency/jboss-as-build-config-7.1.1.Final.jar";
  classpath += ":target/dependency/jboss-as-jms-client-bom-7.1.1.Final.pom";
  classpath += ":target/dependency/jboss-jms-api_1.1_spec-1.0.1.Final.jar";
  classpath += ":target/dependency/jboss-remote-naming-1.0.2.Final.jar";
  classpath += ":target/dependency/netty-3.2.6.Final.jar";
  char *classpathCopy = new char[classpath.size()+1] ;
  strcpy(classpathCopy, classpath.c_str());
  jvmoptions[0].optionString = classpathCopy;
  //jvmoptions[1].optionString = "-Xmx16000m";
  jvmoptions[1].optionString = "-Xmx2048m";
  jvmoptions[2].optionString = "-XX:+UseConcMarkSweepGC";
  memset(&vm_args, 0, sizeof(vm_args));
  vm_args.version = JNI_VERSION_1_4;
  vm_args.nOptions = 1;
  vm_args.options = jvmoptions;
  status = JNI_CreateJavaVM(&jvm, (void**)&env, &vm_args);
  cout << "Result from creating JVM: " << status <<endl;

  if (status != JNI_ERR) {
    cls = env->FindClass("org/jboss/tusk/juniper/InfinispanWriter");
    cout << "Result from finding class: " << cls << endl;
    if(cls !=0) {
      mid = env->GetStaticMethodID(cls, "write", "(Ljava/lang/String;)I");
      cout << "Result from getting method: " << mid << endl;
      jmethodID initMid = env->GetStaticMethodID(cls, "init", "(Ljava/lang/String;)I");;
      jstring dgcPropsFile = env->NewStringUTF(options.jdgProps.c_str());
      int result = env->CallStaticIntMethod(cls, initMid, dgcPropsFile);
      cout << "Result from init method: " << result << endl;
    }
  } else {
    cout << "Cound not init JNI" << endl;
  }
  //end JNI init

  while(!done) {
    //receiver.get(message, Duration::SECOND * 5);
    receiver.get(message);
    if(i == 0) {
      start = now();
      cout << "First message received" << endl;
    }

    //if(i % 10000 == 0) {
    //  cout << i << ". val=" << message.getContent() << endl;
    //}
    //cout << message.getContent() << endl;

    //write message content to data grid
    jstring jstr = env->NewStringUTF(message.getContent().c_str());
    int result = env->CallStaticIntMethod(cls, mid, jstr);
    //cout << "Got result " << result << endl;
    //how many additional times to write this message for ingest; just for testing to increase data load
    /*int multiplier = 1;
    for(int j=1; j < multiplier; j++) {
      result = env->CallStaticIntMethod(cls, mid, jstr);
    }*/

    if(options.ack && (i % options.ack == 0)) {
      session.acknowledge();
    }
    i++;
    if (options.messageCount != 0 && i >= options.messageCount) {
      done = true;
    }
    message = Message();
  }

  //jni cleanup
  cout << "Cleaning up JVM" << endl;
  jmethodID cleanupMid = env->GetStaticMethodID(cls, "cleanup", "()I");;
  int result = env->CallStaticIntMethod(cls, cleanupMid, NULL);
  //cout << "Cleanup result=" << result << endl;
  jvm->DestroyJavaVM();
  cout << "Done cleaning up JVM" << endl;

  end = now();

  if (options.ack) {
    session.acknowledge();
  }

  receiver.close();
}



int main(int argc, char** argv) {
  /*
  if (vm.count("ssl-cert-db")) {
    setenv("QPID_SSL_CERT_DB", vm["ssl-cert-db"].as<string>().c_str(), 1);
    ssl = true;
  }

  if (vm.count("ssl-cert-password-file")) {
    setenv("QPID_SSL_CERT_PASSWORD_FILE", vm["ssl-cert-password-file"].as<string>().c_str(), 1);
  }

  if (vm.count("ssl-cert-name")) {
    setenv("QPID_SSL_CERT_NAME", vm["ssl-cert-name"].as<string>().c_str(), 1);
  }

  int headers = 0;
  if (vm.count("headers")) {
    headers = vm["headers"].as<int>();
  }
  */

  Options options;
  options.parse(argc, argv);
  ptr_vector<Thread> threads;
  ptr_vector<Producer> producers;
  ptr_vector<Consumer> consumers;

  //make random messages
  /*srand(time(NULL));
  Message messages[100];
  for(int j=0; j < 100; j++) {
    string content = "{\"time\":\"2012-11-10 00:47:01\",\"type\":\"c\",\"app\":\"any\",\"orig\":\"ge-1/0/0.120\"";
    content += ",\"origIp\":\"192.168.0." + boost::lexical_cast<string>(rand() % 256) + "\"";
    content += ",\"origPort\":\"" + boost::lexical_cast<string>(rand() % 10 + 10000) + "\"";
    content += ",\"destIp\":\"192.168.0." + boost::lexical_cast<string>(rand() % 256) + "\"";
    content += ",\"destPort\":\"" + boost::lexical_cast<string>(rand() % 10 + 10100) + "\"";
    content += ",\"transIp\":\"192.168.0." + boost::lexical_cast<string>(rand() % 256) + "\"";
    content += ",\"transPort\":\"" + boost::lexical_cast<string>(rand() % 10 + 10200) + "\"}";

    Message message = Message(content);
    for (int i = 0; i < options.headers; i++) {
      message.getProperties()["header" + lexical_cast<string>(i)] = "value" + lexical_cast<string>(i);
    }

    messages[j] = message;
  }*/

  cout << "SendOnly=" << options.sendOnly << endl;
  cout << "ReceiveOnly=" << options.receiveOnly << endl;

  if(!options.sendOnly) {
    for(int i = 0; i < options.numReceivers; i++) {
      Thread* consumerThread = NULL;
      Consumer* consumer = NULL;
      cout << "Starting receiver " << (i+1) << endl;
      Connection c(options.receiverBroker);
      consumer = new Consumer(i, options, c);
      consumers.push_back(consumer);
      if(options.noThreads) {
        consumer->run();
      } else {
        consumerThread = new Thread(consumer);
        threads.push_back(consumerThread);
      }
    }
  }

  if(options.sendOnly) {
    for(int i = 0; i < options.numSenders; i++) {
      Thread* producerThread = NULL;
      Producer* producer = NULL;
      cout << "Starting sender " << (i+1) << endl;
      Connection c(options.senderBroker);
      producer = new Producer(i, options, c);
      producers.push_back(producer);
      if(options.noThreads) {
        producer->run();
      } else {
        producerThread = new Thread(producer);
        threads.push_back(producerThread);
      }
    }
  }

  if(!options.noThreads) {
    for(ptr_vector<Thread>::iterator iter = threads.begin(); iter != threads.end(); iter++) {
      iter->join();
    }
  }

  double senderTotal = 0;
  double receiverTotal = 0;

  for(ptr_vector<Producer>::iterator iter = producers.begin(); iter != producers.end(); iter++) {
    cout << "Sender   " << (iter->index + 1) << " stats: " << iter->stats() << endl;
    senderTotal += iter->total;
  }

  cout << "Sender total: " << senderTotal << " messages/sec" << endl;

  for(ptr_vector<Consumer>::iterator iter = consumers.begin(); iter != consumers.end(); iter++) {
    cout << "Receiver " << (iter->index + 1) << " stats: " << iter->stats() << endl;
    receiverTotal += iter->total;
  }

  cout << "Receiver total: " << receiverTotal << " messages/sec" << endl;
}

