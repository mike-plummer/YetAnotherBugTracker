package edu.psu.yabt.bean;

import java.util.Collection;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import edu.psu.yabt.util.YABTConstants;

/**
 * Performs Repository-related interaction (namely with Subversion repositories).
 * This includes pulling data regarding recent updates.
 */
@Stateless
@LocalBean
public class RepositoryBean {
	private static final Logger logger = LoggerFactory.getLogger(RepositoryBean.class);

	// Number representing the lastest revision to the Subversion repo
	private static final int HEAD_REVISION = -1;
	
	private static SVNRepository repository;

	public RepositoryBean()
	{
		if( repository == null )
		{
			try
			{	//Init connection to SVN repo
				DAVRepositoryFactory.setup( );
				repository = SVNRepositoryFactory.create( SVNURL.parseURIEncoded( YABTConstants.SVN_URL() ) );
				ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager( YABTConstants.SVN_USERNAME(), YABTConstants.SVN_PASSWORD() );
				repository.setAuthenticationManager( authManager );
			}
			catch(Exception e)
			{
				logger.error("Error building SVN connection", e);
			}
		}
	}
	
	/**
	 * Pull data on revisions from specified revision number to HEAD revision. Currently performs
	 * no safety checking in application code on passed-in revision number to validate as positive 
	 * and less than HEAD, so API exceptions are allowed to propagate
	 */
	@SuppressWarnings("unchecked")
	public Collection<SVNLogEntry> getEntriesSinceRevision(int revision) throws SVNException
	{
		return repository.log( new String[] { "" } , null , revision , HEAD_REVISION , true , true );
	}
}
