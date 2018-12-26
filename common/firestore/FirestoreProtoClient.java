/*
 * Copyright 2018 The StartupOS Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.startupos.common.firestore;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.FirebaseApp;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import com.google.protobuf.Message;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.WriteResult;
import java.util.concurrent.ExecutionException;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.InputStream;
import java.io.FileInputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

public class FirestoreProtoClient {
  private static final String PROTO_FIELD = "proto";

  Firestore client;

  public FirestoreProtoClient(String serviceAccountJson) {
    try {
      InputStream serviceAccount = new FileInputStream(serviceAccountJson);
      GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
      FirebaseOptions options = new FirebaseOptions.Builder().setCredentials(credentials).build();
      FirebaseApp.initializeApp(options);
      client = FirestoreClient.getFirestore();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public FirestoreProtoClient(String project, String token) {
    FirebaseOptions options =
        new FirebaseOptions.Builder()
            .setCredentials(GoogleCredentials.create(new AccessToken(token, null)))
            .setProjectId(project)
            .build();
    FirebaseApp.initializeApp(options);
    client = FirestoreClient.getFirestore();
  }

  public Firestore getClient() {
    return client;
  }

  private String joinPath(String collection, String documentId) {
    if (collection.endsWith("/")) {
      return collection + documentId;
    }
    return collection + "/" + documentId;
  }

  private Message parseProto(DocumentSnapshot document, Message.Builder builder)
      throws InvalidProtocolBufferException {
    return builder
        .build()
        .getParserForType()
        .parseFrom(Base64.getDecoder().decode(document.getString(PROTO_FIELD)));
  }

  private CollectionReference getCollectionReference(String[] parts, int length) {
    DocumentReference docRef;
    CollectionReference collectionRef = client.collection(parts[0]);
    for (int i = 1; i < length; i += 2) {
      docRef = collectionRef.document(parts[i]);
      collectionRef = docRef.collection(parts[i + 1]);
    }
    return collectionRef;
  }

  public DocumentReference getDocumentReference(String path) {
    String[] parts = path.split("/");
    if (parts.length % 2 != 0) {
      throw new IllegalArgumentException("Path length should be even but is " + parts.length);
    }
    return getCollectionReference(parts, parts.length - 1).document(parts[parts.length - 1]);
  }

  public DocumentReference getDocumentReference(String collection, String documentId) {
    return getDocumentReference(joinPath(collection, documentId));
  }

  public ApiFuture<DocumentSnapshot> getDocumentAsync(String path) {
    return getDocumentReference(path).get();
  }

  public ApiFuture<DocumentSnapshot> getDocumentAsync(String collection, String documentId) {
    return getDocumentAsync(joinPath(collection, documentId));
  }

  public DocumentSnapshot getDocument(String path) {
    try {
      return getDocumentAsync(path).get();
    } catch (InterruptedException | ExecutionException e) {
      throw new IllegalStateException(e);
    }
  }

  public DocumentSnapshot getDocument(String collection, String documentId) {
    return getDocument(joinPath(collection, documentId));
  }

  public Message getProtoDocument(String path, Message.Builder builder) {
    try {
      return parseProto(getDocument(path), builder);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public Message getProtoDocument(String collection, String documentId, Message.Builder builder) {
    return getProtoDocument(joinPath(collection, documentId), builder);
  }

  public ApiFuture<WriteResult> setDocumentAsync(String path, Map map) {
    return getDocumentReference(path).set(map);
  }

  public ApiFuture<WriteResult> setDocumentAsync(
      String collection, String documentId, Map<String, ?> map) {
    return setDocumentAsync(joinPath(collection, documentId), map);
  }

  public WriteResult setDocument(String path, Map map) {
    try {
      return setDocumentAsync(path, map).get();
    } catch (ExecutionException | InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

  public WriteResult setDocument(String collection, String documentId, Map map) {
    return setDocument(joinPath(collection, documentId), map);
  }

  public ApiFuture<WriteResult> setProtoDocumentAsync(String path, Message proto) {
    byte[] protoBytes = proto.toByteArray();
    String base64BinaryString = Base64.getEncoder().encodeToString(protoBytes);
    return setDocumentAsync(path, ImmutableMap.of(PROTO_FIELD, base64BinaryString));
  }

  public ApiFuture<WriteResult> setProtoDocumentAsync(
      String collection, String documentId, Message proto) {
    return setProtoDocumentAsync(joinPath(collection, documentId), proto);
  }

  public WriteResult setProtoDocument(String path, Message proto) {
    try {
      return setProtoDocumentAsync(path, proto).get();
    } catch (ExecutionException | InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

  public WriteResult setProtoDocument(String collection, String documentId, Message proto) {
    return setProtoDocument(joinPath(collection, documentId), proto);
  }

  public CollectionReference getCollectionReference(String path) {
    String[] parts = path.split("/");
    if (parts.length % 2 != 1) {
      throw new IllegalArgumentException("Path length should be odd but is " + parts.length);
    }
    return getCollectionReference(parts, parts.length);
  }

  public ApiFuture<QuerySnapshot> getDocumentsAsync(String path) {
    return getCollectionReference(path).get();
  }

  public List<Message> getProtoDocuments(String path, Message.Builder builder) {
    ImmutableList.Builder<Message> result = ImmutableList.builder();
    try {
      Message proto = builder.build();
      QuerySnapshot querySnapshot = getDocumentsAsync(path).get();
      for (QueryDocumentSnapshot document : querySnapshot.getDocuments()) {
        result.add(parseProto(document, builder));
      }
      return result.build();
    } catch (ExecutionException | InterruptedException | InvalidProtocolBufferException e) {
      throw new IllegalStateException(e);
    }
  }

  public MessageWithId getDocumentFromCollection(
      String path, Message.Builder builder, boolean shouldRemove) {
    try {
      QuerySnapshot querySnapshot = getCollectionReference(path).limit(1).get().get();
      if (querySnapshot.isEmpty()) {
        return null;
      }
      QueryDocumentSnapshot queryDocumentSnapshot = querySnapshot.getDocuments().get(0);
      if (shouldRemove) {
        deleteDocument(path);
      }
      return MessageWithId.create(
          queryDocumentSnapshot.getId(), parseProto(queryDocumentSnapshot, builder));
    } catch (ExecutionException | InterruptedException | InvalidProtocolBufferException e) {
      throw new IllegalStateException(e);
    }
  }

  public MessageWithId getDocumentFromCollection(String path, Message.Builder proto) {
    return getDocumentFromCollection(path, proto, false);
  }

  public MessageWithId popDocument(String path, Message.Builder proto) {
    return getDocumentFromCollection(path, proto, true);
  }

  public ApiFuture<WriteResult> deleteDocumentAsync(String path) {
    return getDocumentReference(path).delete();
  }

  public WriteResult deleteDocument(String path) {
    try {
      return deleteDocumentAsync(path).get();
    } catch (ExecutionException | InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }
}

