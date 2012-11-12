/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.gateway;

import org.apache.hadoop.gateway.config.GatewayConfigFactory;
import org.apache.hadoop.gateway.jetty.JettyGatewayFactory;
import org.apache.hadoop.gateway.config.Config;
import org.apache.hadoop.gateway.i18n.messages.MessagesFactory;
import org.apache.shiro.web.env.EnvironmentLoaderListener;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class GatewayServer {

  private static GatewayMessages log = MessagesFactory.get( GatewayMessages.class );
  private static GatewayConfig config;
  private static Server jetty;

  //TODO: Need to locate bootstrap config that provides information required to locate the Ambari server.
  //TODO: Provide an XML or JSON equivalent that can be specified directly if Ambari proper is not present.
  public static void main( String[] args ) {
    try {
      log.startingGateway();
      startGateway();
      log.startedGateway( jetty.getConnectors()[ 0 ].getLocalPort() );
    } catch( Exception e ) {
      log.failedToStartGateway( e );
    }
  }

  private static void startGateway() throws Exception {
    config = new GatewayConfig();

    Map<String,String> params = new HashMap<String,String>();
    params.put( "ambari.address", config.getAmbariAddress() );
    params.put( "namenode.address", config.getNameNodeAddress() );
    params.put( "templeton.address", config.getTempletonAddress() );

    //TODO: This needs to be dynamic based on a call to the Ambari server.
    URL configUrl = ClassLoader.getSystemResource( "org/apache/hadoop/gateway/GatewayServer.xml" );
    Config gatewayConfig = GatewayConfigFactory.create( configUrl, params );

    ContextHandlerCollection contexts = new ContextHandlerCollection();
    ServletContextHandler handler = JettyGatewayFactory.create( "gateway/cluster", gatewayConfig );
    handler.getServletContext().setInitParameter( "shiroConfigLocations", config.getShiroConfigFile() );
    handler.addEventListener( new EnvironmentLoaderListener() ); // For Shiro bootstrapping.
    contexts.addHandler( handler );

    jetty = new Server( config.getGatewayPort() );
    jetty.setHandler( contexts );
    jetty.start();
  }

  private static void stopGateway() throws Exception {
    jetty.stop();
    jetty.join();
  }
}
