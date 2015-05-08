package edu.psu.yabt.svn;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.junit.Test;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import edu.psu.yabt.util.YABTConstants;

/**
 * Tests connection to SVN repo to validate ability to pull recent activity
 * 
 * @author mplummer
 */
public class SVNTest 
{	
	/**
	 * Unit Test code adapted from SVNKit Wiki @ http://wiki.svnkit.com/Printing_Out_Repository_History
	 */
	@Test
	public void testSVNConnection() throws Exception
	{
		DAVRepositoryFactory.setup( );

		long startRevision = 0;
		long endRevision = -1; //HEAD (the latest) revision

		SVNRepository repository = null;
		repository = SVNRepositoryFactory.create( SVNURL.parseURIEncoded( YABTConstants.SVN_URL() ) );
		ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager( YABTConstants.SVN_USERNAME(), YABTConstants.SVN_PASSWORD() );
		repository.setAuthenticationManager( authManager );
		Collection<SVNLogEntry> logEntries = null;

		logEntries = repository.log( new String[] { "" } , null , startRevision , endRevision , true , true );
		for ( Iterator<SVNLogEntry> entries = logEntries.iterator( ); entries.hasNext( ); ) {
			SVNLogEntry logEntry = entries.next( );
			System.out.println( "---------------------------------------------" );
			System.out.println ("revision: " + logEntry.getRevision( ) );
			System.out.println( "author: " + logEntry.getAuthor( ) );
			System.out.println( "date: " + logEntry.getDate( ) );
			System.out.println( "log message: " + logEntry.getMessage( ) );

			if ( logEntry.getChangedPaths( ).size( ) > 0 ) 
			{
				System.out.println( "\nchanged paths:" );
				Set<String> changedPathsSet = logEntry.getChangedPaths( ).keySet( );

				for ( Iterator<String> changedPaths = changedPathsSet.iterator( ); changedPaths.hasNext( ); ) {
					SVNLogEntryPath entryPath = ( SVNLogEntryPath ) logEntry.getChangedPaths( ).get( changedPaths.next( ) );
					System.out.println( " "
							+ entryPath.getType( )
							+ " "
							+ entryPath.getPath( )
							+ ( ( entryPath.getCopyPath( ) != null ) ? " (from "
									+ entryPath.getCopyPath( ) + " revision "
									+ entryPath.getCopyRevision( ) + ")" : "" ) );
				}
			}
		}
		
		assertTrue(true);	//Success of this test determined by lack of exceptions. If we made it here, we're good
	}
	
	@Test
	public void testGetRelatedCommits() {
		
	}
}
