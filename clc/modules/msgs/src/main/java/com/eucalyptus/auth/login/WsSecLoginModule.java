/*************************************************************************
 * Copyright 2009-2012 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 *
 * This file may incorporate work covered under the following copyright
 * and permission notice:
 *
 *   Software License Agreement (BSD License)
 *
 *   Copyright (c) 2008, Regents of the University of California
 *   All rights reserved.
 *
 *   Redistribution and use of this software in source and binary forms,
 *   with or without modification, are permitted provided that the
 *   following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer
 *     in the documentation and/or other materials provided with the
 *     distribution.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 *   FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 *   COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 *   INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *   BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *   LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *   LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *   ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *   POSSIBILITY OF SUCH DAMAGE. USERS OF THIS SOFTWARE ACKNOWLEDGE
 *   THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE LICENSED MATERIAL,
 *   COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS SOFTWARE,
 *   AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *   IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA,
 *   SANTA BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY,
 *   WHICH IN THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION,
 *   REPLACEMENT OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO
 *   IDENTIFIED, OR WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT
 *   NEEDED TO COMPLY WITH ANY SUCH LICENSES OR RIGHTS.
 ************************************************************************/

package com.eucalyptus.auth.login;

import java.security.cert.X509Certificate;
import org.apache.log4j.Logger;
import org.apache.xml.security.signature.XMLSignature;
import org.w3c.dom.Element;
import com.eucalyptus.auth.Accounts;
import com.eucalyptus.auth.AuthException;
import com.eucalyptus.auth.api.BaseLoginModule;
import com.eucalyptus.auth.principal.User;
import com.eucalyptus.binding.HoldMe;
import com.eucalyptus.crypto.util.B64;
import com.eucalyptus.crypto.util.WSSecurity;

public class WsSecLoginModule extends BaseLoginModule<WsSecCredentials> {
  private static Logger LOG = Logger.getLogger( WsSecLoginModule.class );
  
  public WsSecLoginModule( ) {}
  
  @Override
  public boolean accepts( ) {
    return super.getCallbackHandler( ) instanceof WsSecCredentials;
  }
  
  @SuppressWarnings( "unchecked" )
  @Override
  public boolean authenticate( final WsSecCredentials wrappedCredentials ) throws Exception {
    HoldMe.canHas.lock( );
    try {
      final Element secNode = WSSecurity.getSecurityElement( wrappedCredentials.getLoginData( ) );
      final XMLSignature sig = WSSecurity.getXMLSignature( secNode );
      final String sigValue = B64.standard.encString(sig.getSignatureValue());
      SecurityContext.enqueueSignature( sigValue );
      
      final X509Certificate cert = WSSecurity.verifySignature( secNode, sig );
      try {
        final User user = Accounts.lookupUserByCertificate( cert );
        super.setCredential( cert );
        super.setPrincipal( user );
        //super.getGroups( ).addAll( Groups.lookupUserGroups( super.getPrincipal( ) ) );
      } catch ( AuthException e ) {
        try {
          if ( !Accounts.lookupCertificate( cert ).isActive( ) ) {
            throw new AuthException( "Certificate is inactive or revoked: " + e.getMessage( ), e );
          } else {
            throw e;
          }
        } catch ( Exception ex ) {
        	// TODO: GRZE should it be "throw ex" instead?
        	throw e;
        }
      }
    } finally {
      HoldMe.canHas.unlock( );
    }
    return true;
  }
  
}
